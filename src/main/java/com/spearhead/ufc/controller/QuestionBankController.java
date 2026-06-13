package com.spearhead.ufc.controller;

import com.spearhead.ufc.dto.OptionDTO;
import com.spearhead.ufc.dto.QuestionListDTO;
import com.spearhead.ufc.model.Option;
import com.spearhead.ufc.model.QuestionBank;
import com.spearhead.ufc.model.RoleQuestionMapping;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.service.QuestionBankService;
import com.spearhead.ufc.service.EmployeeService;
import com.spearhead.ufc.utils.AuthUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/questionBank")
@CrossOrigin(origins = "*")
public class QuestionBankController {

	@Autowired
	private QuestionBankService questionBankService;

	@Autowired
	private AuthUtil authUtil;

	@Autowired
	private EmployeeService employeeService;

	public QuestionBankController(QuestionBankService questionBankService, AuthUtil authUtil,
			EmployeeService employeeService) {
		this.questionBankService = questionBankService;
		this.authUtil = authUtil;
		this.employeeService = employeeService;
	}

	@GetMapping("/getQuestions1")
	public ResponseEntity<Map<String, Object>> getQuestions1(@RequestParam int orgId) {
		log.info("Entered into getQuestions1 method - QuestionBankController");
		Map<String, Object> resp = new HashMap<>();
		try {
			List<QuestionBank> questions = questionBankService.fetchQuestions1(orgId);
			resp.put("success", true);
			resp.put("message", "Questions fetched successfully");
			resp.put("data", questions);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			log.error("Error in getQuestions1 method - QuestionBankController", e.getMessage(), e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch questions: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
		}
	}

	@GetMapping("/list")
	public ResponseEntity<Map<String, Object>> getQuestions(
			@RequestHeader("Authorization") String authHeader,
			@RequestParam(required = false) Integer orgId,
			@RequestParam(required = false) Integer locationId,
			@RequestParam(required = false) Integer roleId) {

		log.info("Entered into getQuestions method - QuestionBankController");
		Map<String, Object> resp = new HashMap<>();
		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				log.warn("User not found or token invalid.");
				resp.put("success", false);
				resp.put("message", "User not found or token invalid. Please login again.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
			}

			Integer finalOrgId = (orgId != null) ? orgId : (user.getOrg() != null ? user.getOrg().getOrgId() : null);
			Integer finalLocationId = (locationId != null) ? locationId
					: (user.getLocation() != null ? user.getLocation().getLocationId() : null);
			Integer finalRoleId = (roleId != null) ? roleId
					: (user.getRoleId() != null ? user.getRoleId().getRoleId() : null);

			List<RoleQuestionMapping> allMappings;
			if (user.getRoleId() != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRoleId().getRoleName())) {
				allMappings = questionBankService.getAllRoleQuestionMappingsWithQuestions();
			} else {
				if (user.getOrg() == null || user.getOrg().getIsActive() == null || !user.getOrg().getIsActive()) {
					log.warn("Employee {} attempted to access questions from inactive organization",
							user.getEmployeeId());
					resp.put("success", false);
					resp.put("message", "Your organization is inactive. You cannot access questions.");
					return ResponseEntity.status(HttpStatus.FORBIDDEN).body(resp);
				}
				allMappings = questionBankService.getQuestionsByFiltersTO(finalOrgId, finalLocationId, null);
			}

			// Group mappings by questionId to avoid duplicates and collect roleNames as array
			Map<Integer, QuestionListDTO> questionMap = new LinkedHashMap<>();
			
			for (RoleQuestionMapping rqm : allMappings) {
				// Filter to only include mappings from active organizations
				if (rqm.getOrg() == null || rqm.getOrg().getIsActive() == null || !rqm.getOrg().getIsActive()) {
					continue;
				}
				if (rqm.getQuestion() == null || rqm.getQuestion().getQuestionId() == null) {
					continue;
				}
				
				Integer questionId = rqm.getQuestion().getQuestionId();
				QuestionBank question = rqm.getQuestion();
				
				if (questionMap.containsKey(questionId)) {
					// Question already exists, just add the role name to the array
					QuestionListDTO existingDto = questionMap.get(questionId);
					if (rqm.getRole() != null && rqm.getRole().getRoleName() != null) {
						String roleName = rqm.getRole().getRoleName();
						if (!existingDto.getRoleNames().contains(roleName)) {
							existingDto.getRoleNames().add(roleName);
						}
					}
					if (rqm.getRole() != null && rqm.getRole().getRoleId() != null) {
						Integer rId = rqm.getRole().getRoleId();
						if (!existingDto.getRoleIds().contains(rId)) {
							existingDto.getRoleIds().add(rId);
						}
					}
				} else {
					// Create new DTO for this question
					String createdName = rqm.getCreatedBy() != null
							? employeeService.getEmployeeFullName(rqm.getCreatedBy())
							: null;
					String updatedName = rqm.getUpdatedBy() != null
							? employeeService.getEmployeeFullName(rqm.getUpdatedBy())
							: null;
					
					List<String> roleNames = new ArrayList<>();
					List<Integer> roleIds = new ArrayList<>();
					if (rqm.getRole() != null && rqm.getRole().getRoleName() != null) {
						roleNames.add(rqm.getRole().getRoleName());
					}
					if (rqm.getRole() != null && rqm.getRole().getRoleId() != null) {
						roleIds.add(rqm.getRole().getRoleId());
					}
					
					QuestionListDTO dto = QuestionListDTO.builder()
							.questionId(questionId)
							.orgId(rqm.getOrg() != null ? rqm.getOrg().getOrgId() : null)
							.orgName(rqm.getOrg() != null ? rqm.getOrg().getOrgName() : null)
							.questionText(question.getQuestionText())
							.questionType(question.getQuestionType())
							.weightage(question.getWeightage())
							.isActive(rqm.getIsActive())
							.fromDate(question.getFromDate())
							.toDate(question.getToDate())
							.createdBy(rqm.getCreatedBy())
							.createdDt(rqm.getCreatedDt())
							.updatedBy(rqm.getUpdatedBy())
							.updatedDt(rqm.getUpdatedDt())
							.createdEmployeeName(createdName)
							.updatedEmployeeName(updatedName)
							.roleNames(roleNames)
							.roleIds(roleIds)
							.build();
					
					questionMap.put(questionId, dto);
				}
			}
			
			List<QuestionListDTO> result = new ArrayList<>(questionMap.values());
			result.sort((a, b) -> {
				OffsetDateTime aTime = a.getUpdatedDt() != null ? a.getUpdatedDt() : a.getCreatedDt();
				OffsetDateTime bTime = b.getUpdatedDt() != null ? b.getUpdatedDt() : b.getCreatedDt();
				if (aTime == null && bTime == null) {
					return 0;
				}
				if (aTime == null) {
					return 1;
				}
				if (bTime == null) {
					return -1;
				}
				return bTime.compareTo(aTime);
			});
			
			resp.put("success", true);
			resp.put("message", "Questions fetched successfully");
			resp.put("data", result);
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			log.error("Error in getQuestions method -QuestionBankController", e.getMessage(), e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch questions: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
		}
	}

	@GetMapping("/details/{questionId}")
	public ResponseEntity<Map<String, Object>> getQuestionDetails(
			@RequestHeader("Authorization") String authHeader,
			@PathVariable Integer questionId) {
		log.info("Entered into getQuestionDetails method - QuestionBankController, questionId={}", questionId);
		Map<String, Object> resp = new HashMap<>();
		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				resp.put("success", false);
				resp.put("message", "User not found or token invalid. Please login again.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
			}

			if (questionId == null) {
				resp.put("success", false);
				resp.put("message", "questionId is required");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
			}

			// Fetch role-question mappings for the given question
			List<RoleQuestionMapping> mappings;
			boolean isSuperAdmin = user.getRoleId() != null &&
					"SUPER_ADMIN".equalsIgnoreCase(user.getRoleId().getRoleName());
			Integer orgId = (user.getOrg() != null ? user.getOrg().getOrgId() : null);

			if (!isSuperAdmin && orgId != null) {
				mappings = questionBankService
						.getRoleQuestionMappingRepository()
						.findByQuestionIdAndOrgId(questionId, orgId);
			} else {
				// For SUPER_ADMIN or users without org context, return active mappings across
				// orgs
				mappings = questionBankService
						.getRoleQuestionMappingRepository()
						.findByQuestionIdActive(questionId);
			}

			// Attach options only for choice-based question types.
			if (mappings != null && !mappings.isEmpty()) {
				QuestionBank question = mappings.get(0).getQuestion();
				Object validAnswerPayload = null;
				if (question != null && question.getValidAnswer() != null && !question.getValidAnswer().isBlank()) {
					List<Integer> validAnswerIds = Arrays.stream(question.getValidAnswer().split(","))
							.map(String::trim)
							.filter(v -> v.matches("\\d+"))
							.map(Integer::valueOf)
							.collect(Collectors.toList());
					if (!validAnswerIds.isEmpty()) {
						validAnswerPayload = validAnswerIds;
					}
				}

				for (RoleQuestionMapping mapping : mappings) {
					mapping.setReasonFlag(question != null ? question.getReasonFlag() : null);
					mapping.setValidAnswer(validAnswerPayload);
				}

				String questionType = question != null ? question.getQuestionType() : null;
				boolean choiceType = questionType != null
						&& ("radio".equalsIgnoreCase(questionType.trim())
								|| "multiselect".equalsIgnoreCase(questionType.trim()));

				if (choiceType) {
					List<Option> options = questionBankService.getOptionsByQuestionId(questionId);
					List<OptionDTO> optionDetails = options.stream()
							.filter(o -> o.getOptionValue() != null && !o.getOptionValue().isBlank())
							.map(o -> new OptionDTO(o.getOptionId(), o.getOptionValue().trim()))
							.collect(Collectors.toList());
					String[] optionValues = options.stream()
							.map(Option::getOptionValue)
							.filter(v -> v != null && !v.isBlank())
							.map(String::trim)
							.toArray(String[]::new);

					for (RoleQuestionMapping mapping : mappings) {
						mapping.setOptions(optionValues);
						mapping.setOptionDetails(optionDetails);
					}
				}
			}

			resp.put("success", true);
			resp.put("message", "Role-Question mappings fetched successfully");
			resp.put("data", mappings);
			return ResponseEntity.ok(resp);
		} catch (IllegalArgumentException iae) {
			resp.put("success", false);
			resp.put("message", iae.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resp);
		} catch (Exception e) {
			log.error("Error in getQuestionDetails - QuestionBankController: {}", e.getMessage(), e);
			resp.put("success", false);
			resp.put("message", "Failed to fetch role-question mappings: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resp);
		}
	}

	@PostMapping("/add")
	public ResponseEntity<Map<String, Object>> addQuestion(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody RoleQuestionMapping dto) {
		log.info("Entered into addQuestion method - QuestionBankController");
		Map<String, Object> response = new HashMap<>();
		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				log.warn("User not found or token invalid during addQuestion.");
				response.put("success", false);
				response.put("message", "User not found or token invalid. Please login again.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			if (dto == null) {
				response.put("success", false);
				response.put("message", "Request body is required");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
			// role validation: either roleId or roleIds must be provided for mapping
			boolean hasSingleRole = (dto.getRole() != null && dto.getRole().getRoleId() != null)
					|| dto.getRoleId() != null;
			boolean hasMultiRoles = dto.getRoleIds() != null && !dto.getRoleIds().isEmpty();
			if (!hasSingleRole && !hasMultiRoles) {
				response.put("success", false);
				response.put("message", "Please select at least one role.");
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
			}
			// Normalize role from flat roleId if provided
			if ((dto.getRole() == null || dto.getRole().getRoleId() == null) && dto.getRoleId() != null) {
				com.spearhead.ufc.model.Role roleRef = new com.spearhead.ufc.model.Role();
				roleRef.setRoleId(dto.getRoleId());
				dto.setRole(roleRef);
			}

			Integer orgId;
			boolean isSuperAdmin = user.getRoleId() != null &&
					"SUPER_ADMIN".equalsIgnoreCase(user.getRoleId().getRoleName());
			if (isSuperAdmin) {
				orgId = (dto.getOrg() != null) ? dto.getOrg().getOrgId() : dto.getOrgId();
				log.info("SUPER_ADMIN: orgId taken from request: {}", orgId);
				if (orgId == null) {
					response.put("success", false);
					response.put("message", "Field 'org.orgId' or 'orgId' is required for SUPER_ADMIN");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			} else {
				orgId = (user.getOrg() != null) ? user.getOrg().getOrgId() : null;
				if (orgId != null) {
					com.spearhead.ufc.model.Org orgRef = new com.spearhead.ufc.model.Org();
					orgRef.setOrgId(orgId);
					dto.setOrg(orgRef);
				}
				log.info("Regular user: orgId taken from employee details: {}", orgId);
				if (orgId == null) {
					response.put("success", false);
					response.put("message", "User does not have an organization assigned");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
			}

			// If questionId is null or 0, create the question first using transient fields
			// Normalize question from flat questionId if provided
			if ((dto.getQuestion() == null || dto.getQuestion().getQuestionId() == null)
					&& dto.getQuestionId() != null) {
				com.spearhead.ufc.model.QuestionBank qref = new com.spearhead.ufc.model.QuestionBank();
				qref.setQuestionId(dto.getQuestionId());
				dto.setQuestion(qref);
			}

			if (dto.getQuestion() == null || dto.getQuestion().getQuestionId() == null) {
				// if (dto.getLocationId() == null) {
				// 	response.put("success", false);
				// 	response.put("message", "Field 'locationId' is required when creating a question");
				// 	return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				// }
				if (dto.getQuestionText() == null || dto.getQuestionText().isBlank()) {
					response.put("success", false);
					response.put("message", "Field 'questionText' is required when creating a question");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
				if (dto.getQuestionType() == null || dto.getQuestionType().isBlank()) {
					response.put("success", false);
					response.put("message", "Field 'questionType' is required when creating a question");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
				if (dto.getWeightage() == null) {
					response.put("success", false);
					response.put("message", "Field 'weightage' is required when creating a question");
					return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
				}
				// Ensure org context on dto
				if (orgId != null) {
					com.spearhead.ufc.model.Org orgRef = new com.spearhead.ufc.model.Org();
					orgRef.setOrgId(orgId);
					dto.setOrg(orgRef);
				}

				// If multiple roles provided, use the first role for QuestionBank.role (model
				// constraint)
				if (dto.getRole() == null || dto.getRole().getRoleId() == null) {
					if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
						com.spearhead.ufc.model.Role roleRef = new com.spearhead.ufc.model.Role();
						roleRef.setRoleId(dto.getRoleIds().get(0));
						dto.setRole(roleRef);
					}
				}

				QuestionBank created = questionBankService.createQuestionFromMapping(dto, user);
				dto.setQuestion(created);
				log.info("Created new question with ID {} during add flow", created.getQuestionId());
			}

			com.spearhead.ufc.model.Org orgObj = null;
			if (orgId != null) {
				orgObj = new com.spearhead.ufc.model.Org();
				orgObj.setOrgId(orgId);
			}

			RoleQuestionMapping mapping = RoleQuestionMapping.builder()
					.org(orgObj)
					.role(dto.getRole())
					.question(dto.getQuestion())
					.isActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE)
					.createdBy(user.getEmployeeId())
					.createdDt(OffsetDateTime.now())
					.build();
			mapping.setOptions(dto.getOptions());
			mapping.setValidAnswer(dto.getValidAnswer());

			log.info("Employee {} attempting to map question {} to role {} for org {}",
					user.getEmployeeId(),
					mapping.getQuestion() != null ? mapping.getQuestion().getQuestionId() : null,
					mapping.getRole() != null ? mapping.getRole().getRoleId() : null,
					orgId);
			QuestionBank savedQuestion = null;
			if (hasMultiRoles) {
				boolean first = true;
				for (Integer rid : dto.getRoleIds()) {
					if (rid == null)
						continue;
					com.spearhead.ufc.model.Role roleRef = new com.spearhead.ufc.model.Role();
					roleRef.setRoleId(rid);
					RoleQuestionMapping each = RoleQuestionMapping.builder()
							.org(orgObj)
							.role(roleRef)
							.question(dto.getQuestion())
							.isActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE)
							.createdBy(user.getEmployeeId())
							.createdDt(OffsetDateTime.now())
							.build();
					// Save options only once to avoid duplicates
					if (first) {
						each.setOptions(dto.getOptions());
						each.setValidAnswer(dto.getValidAnswer());
						first = false;
					}
					savedQuestion = questionBankService.addQuestion(each, user);
				}
			} else {
				savedQuestion = questionBankService.addQuestion(mapping, user);
			}

			response.put("success", true);
			response.put("message", "Question added/mapped successfully");
			response.put("data", savedQuestion);
			return ResponseEntity.ok(response);

		} catch (org.springframework.dao.DataIntegrityViolationException dive) {
			log.error("Data integrity violation in addQuestion - QuestionBankController: {}", dive.getMessage(), dive);
			response.put("success", false);
			response.put("message", "Question already mapped to this role/organization or invalid data");
			return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
		} catch (IllegalArgumentException iae) {
			log.error("Validation error in addQuestion - QuestionBankController: {}", iae.getMessage(), iae);
			response.put("success", false);
			response.put("message", iae.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		} catch (Exception e) {
			log.error("Error in addQuestion method - QuestionBankController: {}", e.getMessage(), e);
			response.put("success", false);
			response.put("message", "Failed to add question: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}

	@RequestMapping(value = "/update", method = { RequestMethod.PUT, RequestMethod.POST })
	public ResponseEntity<Map<String, Object>> updateQuestion(
			@RequestHeader("Authorization") String authHeader,
			@RequestBody RoleQuestionMapping mapping) {
		log.info("Entered into updateQuestion method - QuestionBankController");
		Map<String, Object> response = new HashMap<>();
		try {
			Employee user = authUtil.getEmployeeFromToken(authHeader);
			if (user == null) {
				log.warn("User not found or token invalid during updateQuestion.");
				response.put("success", false);
				response.put("message", "User not found or token invalid. Please login again.");
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
			}

			// Normalize incoming mapping like add API: roleId/roleIds and questionId
			if ((mapping.getRole() == null || mapping.getRole().getRoleId() == null) && mapping.getRoleId() != null) {
				com.spearhead.ufc.model.Role roleRef = new com.spearhead.ufc.model.Role();
				roleRef.setRoleId(mapping.getRoleId());
				mapping.setRole(roleRef);
			}
			if ((mapping.getQuestion() == null || mapping.getQuestion().getQuestionId() == null)
					&& mapping.getQuestionId() != null) {
				com.spearhead.ufc.model.QuestionBank qref = new com.spearhead.ufc.model.QuestionBank();
				qref.setQuestionId(mapping.getQuestionId());
				qref.setReasonFlag(mapping.getReasonFlag());
				mapping.setQuestion(qref);
			}

			Integer orgId;
			if (user.getRoleId() != null && "SUPER_ADMIN".equalsIgnoreCase(user.getRoleId().getRoleName())) {
				orgId = (mapping.getOrg() != null) ? mapping.getOrg().getOrgId() : mapping.getOrgId();
				log.info("SUPER_ADMIN: orgId taken from mapping: {}", orgId);
			} else {
				orgId = (user.getOrg() != null) ? user.getOrg().getOrgId() : null;
				if (orgId != null) {
					com.spearhead.ufc.model.Org orgRef = new com.spearhead.ufc.model.Org();
					orgRef.setOrgId(orgId);
					mapping.setOrg(orgRef); // ensure mapping has correct org
				}
				log.info("Regular user: orgId taken from user details: {}", orgId);
			}

			log.info("Employee {} attempting to update question with ID: {} for orgId: {}", user.getEmployeeId(),
					mapping.getQuestion() != null ? mapping.getQuestion().getQuestionId() : null, orgId);
			QuestionBank updated = questionBankService.updateQuestion(mapping, user);

			// Sync role mappings if roleIds provided (same format as add API)
			if (mapping.getRoleIds() != null && !mapping.getRoleIds().isEmpty()) {
				try {
					java.util.Set<Integer> desired = new java.util.HashSet<>(mapping.getRoleIds());
					java.util.List<RoleQuestionMapping> current = questionBankService
							.getRoleQuestionMappingRepository()
							.findByQuestionIdAndOrgId(updated.getQuestionId(), orgId);

					java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
					// Deactivate those not in desired
					for (RoleQuestionMapping rqm : current) {
						Integer rid = rqm.getRole() != null ? rqm.getRole().getRoleId() : null;
						if (rid != null && !desired.contains(rid) && Boolean.TRUE.equals(rqm.getIsActive())) {
							rqm.setIsActive(false);
							rqm.setUpdatedBy(user.getEmployeeId());
							rqm.setUpdatedDt(now);
							questionBankService.getRoleQuestionMappingRepository().save(rqm);
						}
					}
					// Upsert desired
					for (Integer rid : desired) {
						if (rid == null)
							continue;
						java.util.Optional<RoleQuestionMapping> existing = questionBankService
								.getRoleQuestionMappingRepository()
								.findFirstByOrg_OrgIdAndRole_RoleIdAndQuestion_QuestionId(orgId, rid,
										updated.getQuestionId());
						if (existing.isPresent()) {
							RoleQuestionMapping rqm = existing.get();
							rqm.setIsActive(true);
							rqm.setUpdatedBy(user.getEmployeeId());
							rqm.setUpdatedDt(now);
							questionBankService.getRoleQuestionMappingRepository().save(rqm);
						} else {
							com.spearhead.ufc.model.Role roleRef2 = new com.spearhead.ufc.model.Role();
							roleRef2.setRoleId(rid);
							// Ensure org is always set to avoid NOT NULL constraint violation
							com.spearhead.ufc.model.Org orgForMapping = null;
							if (mapping.getOrg() != null && mapping.getOrg().getOrgId() != null) {
								orgForMapping = mapping.getOrg();
							} else if (orgId != null) {
								orgForMapping = new com.spearhead.ufc.model.Org();
								orgForMapping.setOrgId(orgId);
							} else if (updated.getOrg() != null && updated.getOrg().getOrgId() != null) {
								orgForMapping = updated.getOrg();
							}
							if (orgForMapping == null) {
								log.error(
										"Cannot create role-question mapping: orgId is null for question {} and role {}",
										updated.getQuestionId(), rid);
								return ResponseEntity.status(HttpStatus.BAD_REQUEST)
										.body(java.util.Map.of(
												"success", false,
												"message",
												"Organization context is required to map roles to a question"));
							}
							RoleQuestionMapping rqm = RoleQuestionMapping.builder()
									.org(orgForMapping)
									.role(roleRef2)
									.question(updated)
									.isActive(true)
									.createdBy(user.getEmployeeId())
									.createdDt(now)
									.build();
							questionBankService.getRoleQuestionMappingRepository().save(rqm);
						}
					}
				} catch (Exception roleSyncEx) {
					log.error("Failed to sync role mappings for question {}: {}", updated.getQuestionId(),
							roleSyncEx.getMessage(), roleSyncEx);
					return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
							.body(java.util.Map.of(
									"success", false,
									"message",
									"Question updated but failed to sync roles: " + roleSyncEx.getMessage()));
				}
			}

			if (updated != null && updated.getQuestionId() > 0) {
				response.put("success", true);
				response.put("message", "Question updated successfully");
				response.put("data", updated);
				log.info("Question updated successfully with ID: {}", updated.getQuestionId());
			} else {
				response.put("success", false);
				response.put("message", "Unable to update question");
				response.put("data", null);
				log.warn("Failed to update question with ID: {}",
						mapping.getQuestion() != null ? mapping.getQuestion().getQuestionId() : null);
			}
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException iae) {
			log.error("Validation error in updateQuestion method - QuestionBankController: {}", iae.getMessage(), iae);
			response.put("success", false);
			response.put("message", iae.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
		} catch (Exception e) {
			log.error("Error in updateQuestion method - QuestionBankController: {}", e.getMessage(), e);
			response.put("success", false);
			response.put("message", "Failed to update question: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
		}
	}
}
