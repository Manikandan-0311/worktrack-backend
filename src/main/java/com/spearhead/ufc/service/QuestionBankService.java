package com.spearhead.ufc.service;

import com.spearhead.ufc.dto.QuestionBankDTO;
import com.spearhead.ufc.model.QuestionBank;
import com.spearhead.ufc.repository.OrgRepository;
import com.spearhead.ufc.repository.QuestionBankRepository;
import com.spearhead.ufc.repository.RoleQuestionMappingRepository;
import com.spearhead.ufc.repository.OptionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;
import com.spearhead.ufc.dto.RoleQuestionMapTO;
import com.spearhead.ufc.model.RoleQuestionMapping;
import com.spearhead.ufc.model.Employee;
import com.spearhead.ufc.model.Org;
import com.spearhead.ufc.model.Location;
import com.spearhead.ufc.model.Role;
import com.spearhead.ufc.model.Option;

@Slf4j
@Service
public class QuestionBankService {

	@Autowired
	private QuestionBankRepository questionBankRepository;

	@Autowired
	private OrgRepository orgRepository;

	@Autowired
	private RoleQuestionMappingRepository roleQuestionMappingRepository;

	@Autowired
	private OptionRepository optionRepository;

	public QuestionBankService(QuestionBankRepository questionBankRepository) {
		this.questionBankRepository = questionBankRepository;
	}

	// Expose repository for controller sync where needed
	public RoleQuestionMappingRepository getRoleQuestionMappingRepository() {
		return roleQuestionMappingRepository;
	}

	public List<QuestionBank> fetchQuestions1(int orgId) {
		log.info("Entered into fetchQuestions1 method - QuestionBankService");
		try {
			List<QuestionBank> questions = questionBankRepository.findActiveQuestions1(orgId);
			return questions;
		} catch (Exception e) {
			log.error("Error occurred in fetchQuestions1 method - QuestionBankService: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch questions for orgId: " + orgId, e);
		} finally {
			log.info("Exited from fetchQuestions1 method - QuestionBankService");
		}
	}

	public List<QuestionBankDTO> getQuestions(Integer orgId) {
		log.info("Entered into getQuestions method - QuestionBankService");
		try {
			List<QuestionBankDTO> questions = questionBankRepository.findQuestionsByFilters(orgId);
			return questions;
		} catch (Exception e) {
			log.error("Error occurred in getQuestions method - QuestionBankService: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch question list for orgId: " + orgId, e);
		} finally {
			log.info("Exited from getQuestions method - QuestionBankService");
		}
	}

	public QuestionBank addQuestion(RoleQuestionMapping mapping, Employee user) {
		log.info("Entered into addQuestion method - QuestionBankService for employee: " +
				(user != null ? user.getEmployeeId() : null));
		try {
			if (mapping == null) {
				throw new IllegalArgumentException("Mapping must not be null");
			}
			if (mapping.getQuestion() == null || mapping.getQuestion().getQuestionId() == null) {
				throw new IllegalArgumentException("Question must not be null in mapping");
			}

			    // Fetch existing question to return and ensure it exists
			QuestionBank question = questionBankRepository
					.findById(mapping.getQuestion().getQuestionId().longValue())
					.orElseThrow(() -> new RuntimeException(
							"Question not found with ID: " + mapping.getQuestion().getQuestionId()));

			// Ensure orgId is set on the mapping based on role/user context
			if (mapping.getOrg() == null) {
				Integer orgId = (user != null && user.getOrg() != null) ? user.getOrg().getOrgId() : null;
				if (orgId != null) {
					Org orgRef = new Org();
					orgRef.setOrgId(orgId);
					mapping.setOrg(orgRef);
					log.info("Mapping org set from user details: {}", orgId);
				}
			}

			// Default active flag
			if (mapping.getIsActive() == null) {
				mapping.setIsActive(true);
			}

			// Persist the role-question mapping
			roleQuestionMappingRepository.save(mapping);
			    log.info("Role-Question mapping saved for questionId: {}, roleId: {}",
				    mapping.getQuestion() != null ? mapping.getQuestion().getQuestionId() : null,
				    mapping.getRole() != null ? mapping.getRole().getRoleId() : null);

			// If question type requires options, persist them (support object array)
			if (question.getQuestionType() != null) {
				String qt = question.getQuestionType().trim().toLowerCase();
				if (("radio".equals(qt) || "multiselect".equals(qt)) && mapping.getOptions() != null) {
					java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
					List<Option> savedOptions = new ArrayList<>();
					// Accept both array of strings and array of objects
					Object[] optionsArr = (Object[]) mapping.getOptions();
					for (Object optObj : optionsArr) {
						String value = null;
						Boolean isActive = Boolean.TRUE;
						if (optObj instanceof Map<?, ?> map) {
							Object v = map.get("optionValue");
							if (v == null) v = map.get("optionsValue");
							if (v != null && !String.valueOf(v).isBlank()) value = String.valueOf(v).trim();
						} else if (optObj != null && !optObj.toString().isBlank()) {
							value = optObj.toString().trim();
						}
						if (value == null || value.isBlank()) continue;
						Option opt = Option.builder()
								.org(question.getOrg())
								.question(question)
								.optionValue(value)
								.isActive(isActive)
								.createdBy(user != null ? user.getEmployeeId() : null)
								.createdDt(now)
								.build();
						savedOptions.add(optionRepository.save(opt));
					}
					log.info("Saved {} options for questionId {}", savedOptions.size(), question.getQuestionId());

					if (mapping.getValidAnswer() != null) {
						List<Option> activeOptions = optionRepository
								.findByQuestion_QuestionIdAndIsActiveTrue(question.getQuestionId());
						String validAnswerOptionIdsCsv = resolveValidAnswerOptionIdsCsv(mapping.getValidAnswer(), activeOptions);
						question.setValidAnswer(validAnswerOptionIdsCsv);
						questionBankRepository.save(question);
					}
				}
			}
			return question;
		} catch (Exception e) {
			    log.error("Error occurred in addQuestion method - QuestionBankService: {}", e.getMessage(), e);
			    throw new RuntimeException("Failed to add question mapping for questionId: " +
				    (mapping != null && mapping.getQuestion() != null ? mapping.getQuestion().getQuestionId() : null), e);
		} finally {
			log.info("Exited from addQuestion method - QuestionBankService");
		}
	}

	public QuestionBank updateQuestion(RoleQuestionMapping mapping, Employee user) {
		log.info("Entered into updateQuestion method - QuestionBankService for employee: " +
				(user != null ? user.getEmployeeId() : null));
		try {
				if (mapping.getQuestion() == null || mapping.getQuestion().getQuestionId() == null) {
				log.error("QuestionId must not be null for mapping");
				throw new IllegalArgumentException("QuestionId must not be null");
			}
			    QuestionBank existing = questionBankRepository
				    .findById(mapping.getQuestion().getQuestionId().longValue())
				    .orElseThrow(() -> new RuntimeException(
					    "Question not found with ID: " + mapping.getQuestion().getQuestionId()));

			// Update question fields from mapping transient values if provided
			if (mapping.getQuestionText() != null && !mapping.getQuestionText().isBlank()) {
				existing.setQuestionText(mapping.getQuestionText().trim());
			}
			if (mapping.getQuestionType() != null && !mapping.getQuestionType().isBlank()) {
				existing.setQuestionType(mapping.getQuestionType().trim());
			}
			if (mapping.getWeightage() != null) {
				existing.setWeightage(mapping.getWeightage());
			}
			if (mapping.getIsActive() != null) {
				existing.setIsActive(mapping.getIsActive());
			}
			if (mapping.getFromDate() != null) {
				existing.setFromDate(mapping.getFromDate());
			}
			if (mapping.getToDate() != null) {
				existing.setToDate(mapping.getToDate());
			}
			if (mapping.getReasonFlag() != null) {
				existing.setReasonFlag(mapping.getReasonFlag());
			} else if (mapping.getQuestion() != null && mapping.getQuestion().getReasonFlag() != null) {
				existing.setReasonFlag(mapping.getQuestion().getReasonFlag());
			}
			if (mapping.getLocationId() != null) {
				Location locRef = new Location();
				locRef.setLocationId(mapping.getLocationId());
			}
			if (mapping.getOrg() != null) {
				existing.setOrg(mapping.getOrg());
			}
			if (mapping.getRole() != null && mapping.getRole().getRoleId() != null) {
				existing.setRole(mapping.getRole());
			} else if (mapping.getRoleId() != null) {
				Role roleRef = new Role();
				roleRef.setRoleId(mapping.getRoleId());
				existing.setRole(roleRef);
			}

			existing.setUpdatedBy(user != null ? user.getEmployeeId() : null);
			existing.setUpdatedDt(java.time.OffsetDateTime.now());

			QuestionBank updated = questionBankRepository.save(existing);
			log.info("Question updated successfully with ID: {}", updated.getQuestionId());

			// Update role-question mapping if roleId is provided
			if (mapping.getRole() != null && mapping.getRole().getRoleId() != null) {
				// Ensure mapping has required associations before saving
				if (mapping.getQuestion() == null || mapping.getQuestion().getQuestionId() == null) {
					mapping.setQuestion(updated);
				}
				if (mapping.getOrg() == null) {
					mapping.setOrg(updated.getOrg());
				}
				try {
					Integer orgIdRqm = mapping.getOrg() != null ? mapping.getOrg().getOrgId() : (updated.getOrg() != null ? updated.getOrg().getOrgId() : null);
					Integer roleIdRqm = mapping.getRole().getRoleId();
					Integer questionIdRqm = updated.getQuestionId();
					java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

					if (orgIdRqm != null && roleIdRqm != null && questionIdRqm != null) {
						java.util.Optional<RoleQuestionMapping> existingRqmOpt = roleQuestionMappingRepository
								.findFirstByOrg_OrgIdAndRole_RoleIdAndQuestion_QuestionId(orgIdRqm, roleIdRqm, questionIdRqm);
						if (existingRqmOpt.isPresent()) {
							RoleQuestionMapping rqm = existingRqmOpt.get();
							if (mapping.getIsActive() != null) {
								rqm.setIsActive(mapping.getIsActive());
							}
							rqm.setUpdatedBy(user != null ? user.getEmployeeId() : null);
							rqm.setUpdatedDt(now);
							roleQuestionMappingRepository.save(rqm);
							log.info("Role-Question mapping reused and updated for orgId={}, roleId={}, questionId={}", orgIdRqm, roleIdRqm, questionIdRqm);
						} else {
							mapping.setIsActive(mapping.getIsActive() != null ? mapping.getIsActive() : true);
							mapping.setCreatedBy(user != null ? user.getEmployeeId() : null);
							mapping.setCreatedDt(now);
							roleQuestionMappingRepository.save(mapping);
							log.info("Role-Question mapping created for orgId={}, roleId={}, questionId={}", orgIdRqm, roleIdRqm, questionIdRqm);
						}
					} else {
						log.warn("Skipping role-question mapping update: missing orgId/roleId/questionId");
					}
				} catch (Exception mappingException) {
					log.error("Failed to update role-question mapping for questionId: {}", updated.getQuestionId(), mappingException);
					throw new RuntimeException(
						"Question updated but failed to update role mapping: " + mappingException.getMessage(),
						mappingException);
				}
			}

			// Upsert options when question type is choice-based
			String qt = updated.getQuestionType() != null ? updated.getQuestionType().trim().toLowerCase() : null;
			if (qt != null && ("radio".equals(qt) || "multiselect".equals(qt)) && mapping.getOptions() != null) {
				try {
					List<Option> existingOptions = optionRepository
							.findByQuestion_QuestionIdAndIsActiveTrue(updated.getQuestionId());
					String[] newOptions = mapping.getOptions();
					java.time.OffsetDateTime now = java.time.OffsetDateTime.now();

					int common = Math.min(existingOptions.size(), newOptions.length);
					for (int i = 0; i < common; i++) {
						Option opt = existingOptions.get(i);
						String val = newOptions[i];
						if (val != null && !val.isBlank()) {
							opt.setOptionValue(val.trim());
							opt.setUpdatedBy(user != null ? user.getEmployeeId() : null);
							opt.setUpdatedDt(now);
							optionRepository.save(opt);
						}
					}

					for (int i = common; i < existingOptions.size(); i++) {
						Option opt = existingOptions.get(i);
						opt.setIsActive(Boolean.FALSE);
						opt.setUpdatedBy(user != null ? user.getEmployeeId() : null);
						opt.setUpdatedDt(now);
						optionRepository.save(opt);
					}

					for (int i = common; i < newOptions.length; i++) {
						String val = newOptions[i];
						if (val == null || val.isBlank()) continue;
						Option opt = Option.builder()
								.org(updated.getOrg())
								.question(updated)
								.optionValue(val.trim())
								.isActive(Boolean.TRUE)
								.createdBy(user != null ? user.getEmployeeId() : null)
								.createdDt(now)
								.build();
						optionRepository.save(opt);
					}

					log.info("Options upserted for questionId {}: updated={}, deactivated={}, created={}",
							updated.getQuestionId(),
							common,
							existingOptions.size() - common,
							newOptions.length - common);

					List<Integer> duplicateIds = removeDuplicateOptionsByValue(updated.getQuestionId());
					if (!duplicateIds.isEmpty()) {
						String cleaned = removeDeletedIdsFromCsv(updated.getValidAnswer(), duplicateIds);
						updated.setValidAnswer(cleaned);
						updated.setUpdatedBy(user != null ? user.getEmployeeId() : null);
						updated.setUpdatedDt(java.time.OffsetDateTime.now());
						updated = questionBankRepository.save(updated);
						log.info("Removed duplicate options for questionId {} and cleaned valid_answer: {}",
								updated.getQuestionId(), cleaned);
					}
				} catch (Exception optException) {
					log.error("Failed to upsert options for questionId {}: {}", updated.getQuestionId(), optException.getMessage(), optException);
					throw new RuntimeException("Question updated but failed to upsert options: " + optException.getMessage(), optException);
				}
			}

			if (mapping.getValidAnswer() != null) {
				List<Option> allOptions = optionRepository.findByQuestion_QuestionId(updated.getQuestionId());
				applyDeletedOptionsFromRequest(mapping.getDeletedOptionIds(), allOptions, user);
				allOptions = optionRepository.findByQuestion_QuestionId(updated.getQuestionId());
				String validAnswerOptionIdsCsv = mergeValidAnswerOptionIdsCsv(
						updated.getValidAnswer(),
						mapping.getValidAnswer(),
						allOptions);
				updated.setValidAnswer(validAnswerOptionIdsCsv);
				updated.setUpdatedBy(user != null ? user.getEmployeeId() : null);
				updated.setUpdatedDt(java.time.OffsetDateTime.now());
				updated = questionBankRepository.save(updated);
				log.info("Updated valid_answer as option-id CSV for questionId {}: {}", updated.getQuestionId(), validAnswerOptionIdsCsv);
			} else if (mapping.getDeletedOptionIds() != null && !mapping.getDeletedOptionIds().isEmpty()) {
				List<Option> allOptions = optionRepository.findByQuestion_QuestionId(updated.getQuestionId());
				applyDeletedOptionsFromRequest(mapping.getDeletedOptionIds(), allOptions, user);
				String cleaned = removeDeletedIdsFromCsv(updated.getValidAnswer(), mapping.getDeletedOptionIds());
				updated.setValidAnswer(cleaned);
				updated.setUpdatedBy(user != null ? user.getEmployeeId() : null);
				updated.setUpdatedDt(java.time.OffsetDateTime.now());
				updated = questionBankRepository.save(updated);
				log.info("Removed deleted option ids from valid_answer for questionId {}: {}", updated.getQuestionId(), cleaned);
			}

			return updated;
		} catch (Exception e) {
			log.error("Error occurred in updateQuestion method - QuestionBankService: {}", e.getMessage(), e);
			    throw new RuntimeException("Failed to update question with ID: " +
				    (mapping != null && mapping.getQuestion() != null ? mapping.getQuestion().getQuestionId() : null), e);
		} finally {
			log.info("Exited from updateQuestion method - QuestionBankService");
		}
	}

	public List<QuestionBank> getAllQuestionsTO() {
		log.info("Entered into getAllQuestionsTO method - QuestionBankService");
		try {
			List<QuestionBank> questions = questionBankRepository.findAllQuestionsTO();
			log.info("Fetched {} questions in getAllQuestionsTO", questions != null ? questions.size() : 0);
			return questions;
		} catch (Exception e) {
			log.error("Error occurred in getAllQuestionsTO method - QuestionBankService: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch all questions", e);
		} finally {
			log.info("Exited from getAllQuestionsTO method - QuestionBankService");
		}
	}

	public List<RoleQuestionMapping> getAllRoleQuestionMappingsWithQuestions() {
		log.info("Entered into getAllRoleQuestionMappingsWithQuestions method - QuestionBankService");
		try {
			List<RoleQuestionMapping> mappings = roleQuestionMappingRepository.findAllActiveRoleQuestionMappings();
			if (mappings == null || mappings.isEmpty()) {
				log.info("No active role-question mappings found");
				return List.of();
			}
			log.info("Fetched {} role-question mappings in getAllRoleQuestionMappingsWithQuestions", mappings.size());
			return mappings;
		} catch (Exception e) {
			log.error("Error occurred in getAllRoleQuestionMappingsWithQuestions method - QuestionBankService: {}",
					e.getMessage(), e);
			throw new RuntimeException("Failed to fetch all role-question mappings", e);
		} finally {
			log.info("Exited from getAllRoleQuestionMappingsWithQuestions method - QuestionBankService");
		}
	}

	public List<RoleQuestionMapping> getQuestionsByFiltersTO(Integer orgId, Integer locationId, Integer roleId) {
		log.info(
				"Entered into getQuestionsByFiltersTO method - QuestionBankService with orgId={}, locationId={}, roleId={}",
				orgId, locationId, roleId);
		try {
			List<RoleQuestionMapping> mappings;
			
			// If roleId is provided, fetch by roleId; otherwise fetch by orgId
			if (roleId != null) {
				mappings = roleQuestionMappingRepository
						.findByRoleIdAndIsActiveTrue(roleId);
				if (mappings == null || mappings.isEmpty()) {
					log.info("No role-question mappings found for roleId={}", roleId);
					return List.of();
				}
			} else if (orgId != null) {
				mappings = roleQuestionMappingRepository
						.findByOrg_OrgIdAndIsActiveTrue(orgId);
				if (mappings == null || mappings.isEmpty()) {
					log.info("No role-question mappings found for orgId={}", orgId);
					return List.of();
				}
			} else {
				log.warn("Both roleId and orgId are null in getQuestionsByFiltersTO");
				return List.of();
			}
			
			log.info("Fetched {} role-question mappings in getQuestionsByFiltersTO", mappings.size());
			return mappings;
		} catch (Exception e) {
			log.error("Error occurred in getQuestionsByFiltersTO method - QuestionBankService: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch questions by filters", e);
		} finally {
			log.info("Exited from getQuestionsByFiltersTO method - QuestionBankService");
		}
	}

	public List<Option> getOptionsByQuestionId(Integer questionId) {
		log.info("Entered into getOptionsByQuestionId - QuestionBankService, questionId={}", questionId);
		if (questionId == null) {
			throw new IllegalArgumentException("questionId is required");
		}
		try {
			List<Option> options = optionRepository.findByQuestion_QuestionId(questionId);
			log.info("Fetched {} options for questionId {}", options != null ? options.size() : 0, questionId);
			return options != null ? options : List.of();
		} catch (Exception e) {
			log.error("Error in getOptionsByQuestionId: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to fetch options for questionId: " + questionId, e);
		}
	}

		public QuestionBank createQuestion(QuestionBankDTO dto, Employee user) {
			log.info("Entered into createQuestion - QuestionBankService for employee: " +
					(user != null ? user.getEmployeeId() : null));
			try {
				if (dto == null) {
					throw new IllegalArgumentException("Request body is required");
				}
				if (dto.getQuestionText() == null || dto.getQuestionText().isBlank()) {
					throw new IllegalArgumentException("Field 'questionText' is required");
				}
				if (dto.getQuestionType() == null || dto.getQuestionType().isBlank()) {
					throw new IllegalArgumentException("Field 'questionType' is required");
				}
				if (dto.getWeightage() == null) {
					throw new IllegalArgumentException("Field 'weightage' is required and must be a number");
				}

				Integer resolvedOrgId = dto.getOrgId();
				boolean isSuperAdmin = user != null && user.getRoleId() != null
					&& "SUPER_ADMIN".equalsIgnoreCase(user.getRoleId().getRoleName());
				if (!isSuperAdmin) {
					resolvedOrgId = (user != null && user.getOrg() != null) ? user.getOrg().getOrgId() : null;
				}
				if (resolvedOrgId == null) {
					throw new IllegalArgumentException("User does not have an organization assigned");
				}
				if (dto.getLocationId() == null) {
					throw new IllegalArgumentException("Field 'locationId' is required");
				}
				if (dto.getRoleId() == null) {
					throw new IllegalArgumentException("Field 'roleId' is required");
				}

				Org orgRef = new Org();
				orgRef.setOrgId(resolvedOrgId);
			Role roleRef = new Role();
			roleRef.setRoleId(dto.getRoleId());

			QuestionBank qb = QuestionBank.builder()
					.org(orgRef)
						.questionType(dto.getQuestionType().trim())
						.weightage(dto.getWeightage())
						.isActive(dto.getIsActive() != null ? dto.getIsActive() : Boolean.TRUE)
						.createdBy(user != null ? user.getEmployeeId() : null)
						.createdDt(java.time.OffsetDateTime.now())
						.build();

				QuestionBank saved = questionBankRepository.save(qb);
				log.info("Created question with ID: {}", saved.getQuestionId());
				return saved;
			} catch (IllegalArgumentException iae) {
				throw iae;
			} catch (RuntimeException re) {
				throw re;
			} catch (Exception e) {
				log.error("Error in createQuestion - QuestionBankService: {}", e.getMessage(), e);
				throw new RuntimeException("Failed to create question: " + e.getMessage(), e);
			} finally {
				log.info("Exited from createQuestion - QuestionBankService");
			}
		}


	public QuestionBank createQuestionFromMapping(RoleQuestionMapping mapping, Employee user) {
		log.info("Entered into createQuestionFromMapping - QuestionBankService for employee: " +
				(user != null ? user.getEmployeeId() : null));
		if (mapping == null) {
			throw new IllegalArgumentException("Request body is required");
		}
		if (mapping.getQuestionText() == null || mapping.getQuestionText().isBlank()) {
			throw new IllegalArgumentException("Field 'questionText' is required when creating a question");
		}
		if (mapping.getQuestionType() == null || mapping.getQuestionType().isBlank()) {
			throw new IllegalArgumentException("Field 'questionType' is required when creating a question");
		}
		if (mapping.getWeightage() == null) {
			throw new IllegalArgumentException("Field 'weightage' is required when creating a question");
		}
		if (mapping.getRole() == null || mapping.getRole().getRoleId() == null) {
			throw new IllegalArgumentException("Field 'role.roleId' is required");
		}

		Integer resolvedOrgId = (mapping.getOrg() != null) ? mapping.getOrg().getOrgId() : null;
		boolean isSuperAdmin = user != null && user.getRoleId() != null
			&& "SUPER_ADMIN".equalsIgnoreCase(user.getRoleId().getRoleName());
		if (!isSuperAdmin) {
			resolvedOrgId = (user != null && user.getOrg() != null) ? user.getOrg().getOrgId() : null;
		}
		if (resolvedOrgId == null) {
			throw new IllegalArgumentException("User does not have an organization assigned");
		}

		Org orgRef = new Org();
		orgRef.setOrgId(resolvedOrgId);
		Role roleRef = new Role();
		roleRef.setRoleId(mapping.getRole() != null ? mapping.getRole().getRoleId() : null);

		QuestionBank qb = QuestionBank.builder()
				.org(orgRef)
				.role(roleRef)
				.questionText(mapping.getQuestionText().trim())
				.questionType(mapping.getQuestionType().trim())
				.weightage(mapping.getWeightage())
				.reasonFlag(mapping.getReasonFlag())
				.isActive(mapping.getIsActive() != null ? mapping.getIsActive() : Boolean.TRUE)
				.createdBy(user != null ? user.getEmployeeId() : null)
				.createdDt(java.time.OffsetDateTime.now())
				.build();

		if (mapping.getFromDate() != null) qb.setFromDate(mapping.getFromDate());
		if (mapping.getToDate() != null) qb.setToDate(mapping.getToDate());

		QuestionBank saved = questionBankRepository.save(qb);
		log.info("Created question with ID: {}", saved.getQuestionId());
		return saved;
	}

	private String resolveValidAnswerOptionIdsCsv(Object rawValidAnswer, List<Option> activeOptions) {
		if (rawValidAnswer == null) {
			return null;
		}

		Map<Integer, Option> optionById = new HashMap<>();
		Map<String, Integer> optionIdByValue = new HashMap<>();
		if (activeOptions != null) {
			for (Option opt : activeOptions) {
				if (opt == null) {
					continue;
				}
				optionById.put(opt.getOptionId(), opt);
				String value = opt.getOptionValue();
				if (value != null && !value.isBlank()) {
					optionIdByValue.put(value.trim().toLowerCase(), opt.getOptionId());
				}
			}
		}

		List<String> tokens = new ArrayList<>();
		if (rawValidAnswer instanceof List<?>) {
			for (Object item : (List<?>) rawValidAnswer) {
				if (item == null) {
					continue;
				}
				if (item instanceof Map<?, ?> mapItem) {
					Boolean isValidAnswer = toBooleanValue(mapItem.get("isValidAnswer"));
					if (isValidAnswer != null && !isValidAnswer) {
						continue;
					}
					Object idObj = mapItem.get("optionId");
					Object valueObj = mapItem.get("optionValue");
					if (valueObj == null) {
						valueObj = mapItem.get("optionsValue");
					}
					if (idObj != null && !String.valueOf(idObj).isBlank()) {
						tokens.add(String.valueOf(idObj));
					} else if (valueObj != null && !String.valueOf(valueObj).isBlank()) {
						tokens.add(String.valueOf(valueObj));
					}
				} else {
					tokens.add(String.valueOf(item));
				}
			}
		} else if (rawValidAnswer instanceof Map<?, ?> mapAnswer) {
			Boolean isValidAnswer = toBooleanValue(mapAnswer.get("isValidAnswer"));
			if (isValidAnswer != null && !isValidAnswer) {
				return null;
			}
			Object idObj = mapAnswer.get("optionId");
			Object valueObj = mapAnswer.get("optionValue");
			if (valueObj == null) {
				valueObj = mapAnswer.get("optionsValue");
			}
			if (idObj != null && !String.valueOf(idObj).isBlank()) {
				tokens.add(String.valueOf(idObj));
			} else if (valueObj != null && !String.valueOf(valueObj).isBlank()) {
				tokens.add(String.valueOf(valueObj));
			}
		} else {
			String raw = String.valueOf(rawValidAnswer);
			if (!raw.isBlank()) {
				for (String part : raw.split(",")) {
					tokens.add(part);
				}
			}
		}

		Set<Integer> resolvedIds = new LinkedHashSet<>();
		for (String token : tokens) {
			if (token == null || token.isBlank()) {
				continue;
			}
			String trimmed = token.trim();
			if (trimmed.matches("\\d+")) {
				Integer id = Integer.valueOf(trimmed);
				if (!optionById.containsKey(id)) {
					throw new IllegalArgumentException("Invalid validAnswer option id: " + id + " for this question");
				}
				resolvedIds.add(id);
			} else {
				Integer resolved = optionIdByValue.get(trimmed.toLowerCase());
				if (resolved == null) {
					throw new IllegalArgumentException("Invalid validAnswer option value: " + trimmed);
				}
				resolvedIds.add(resolved);
			}
		}

		if (resolvedIds.isEmpty()) {
			return null;
		}

		return resolvedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	private String mergeValidAnswerOptionIdsCsv(String existingCsv, Object rawValidAnswer, List<Option> allOptions) {
		Map<Integer, Option> optionById = new HashMap<>();
		Map<String, Integer> optionIdByValue = new HashMap<>();
		if (allOptions != null) {
			for (Option opt : allOptions) {
				if (opt == null) {
					continue;
				}
				optionById.put(opt.getOptionId(), opt);
				String value = opt.getOptionValue();
				if (value != null && !value.isBlank()) {
					optionIdByValue.put(value.trim().toLowerCase(), opt.getOptionId());
				}
			}
		}

		Set<Integer> mergedIds = new LinkedHashSet<>();
		if (existingCsv != null && !existingCsv.isBlank()) {
			for (String part : existingCsv.split(",")) {
				if (part == null || part.isBlank()) {
					continue;
				}
				String trimmed = part.trim();
				if (trimmed.matches("\\d+")) {
					Integer id = Integer.valueOf(trimmed);
					if (optionById.containsKey(id)) {
						mergedIds.add(id);
					}
				}
			}
		}

		if (rawValidAnswer instanceof List<?>) {
			for (Object item : (List<?>) rawValidAnswer) {
				applyValidAnswerOperation(item, mergedIds, optionById, optionIdByValue);
			}
		} else if (rawValidAnswer instanceof Map<?, ?> || rawValidAnswer != null) {
			applyValidAnswerOperation(rawValidAnswer, mergedIds, optionById, optionIdByValue);
		}

		if (mergedIds.isEmpty()) {
			return null;
		}
		return mergedIds.stream().map(String::valueOf).collect(Collectors.joining(","));
	}

	private void applyValidAnswerOperation(Object item, Set<Integer> mergedIds,
			Map<Integer, Option> optionById, Map<String, Integer> optionIdByValue) {
		if (item == null) {
			return;
		}

		Integer resolvedId = null;
		Boolean isValidFlag = null;

		if (item instanceof Map<?, ?> mapItem) {
			isValidFlag = toBooleanValue(mapItem.get("isValidAnswer"));
			Object idObj = mapItem.get("optionId");
			Object valueObj = mapItem.get("optionValue");
			if (valueObj == null) {
				valueObj = mapItem.get("optionsValue");
			}
			if (idObj != null && !String.valueOf(idObj).isBlank() && String.valueOf(idObj).trim().matches("\\d+")) {
				Integer idCandidate = Integer.valueOf(String.valueOf(idObj).trim());
				if (optionById.containsKey(idCandidate)) {
					resolvedId = idCandidate;
				}
			}
			if (resolvedId == null && valueObj != null && !String.valueOf(valueObj).isBlank()) {
				resolvedId = optionIdByValue.get(String.valueOf(valueObj).trim().toLowerCase());
			}
		} else {
			String raw = String.valueOf(item).trim();
			if (!raw.isBlank()) {
				if (raw.matches("\\d+")) {
					resolvedId = Integer.valueOf(raw);
				} else {
					resolvedId = optionIdByValue.get(raw.toLowerCase());
				}
			}
		}

		if (resolvedId == null || !optionById.containsKey(resolvedId)) {
			log.warn("Skipping invalid validAnswer entry for question update: {}", item);
			return;
		}

		if (Boolean.FALSE.equals(isValidFlag)) {
			mergedIds.remove(resolvedId);
		} else {
			mergedIds.add(resolvedId);
		}
	}

	private void applyOptionActiveFlagsFromValidAnswer(Object rawValidAnswer, List<Option> allOptions, Employee user) {
		if (!(rawValidAnswer instanceof List<?>) || allOptions == null || allOptions.isEmpty()) {
			return;
		}

		Map<Integer, Option> optionById = new HashMap<>();
		Map<String, Option> optionByValue = new HashMap<>();
		for (Option opt : allOptions) {
			if (opt == null) {
				continue;
			}
			optionById.put(opt.getOptionId(), opt);
			if (opt.getOptionValue() != null && !opt.getOptionValue().isBlank()) {
				optionByValue.put(opt.getOptionValue().trim().toLowerCase(), opt);
			}
		}

		java.time.OffsetDateTime now = java.time.OffsetDateTime.now();
		for (Object item : (List<?>) rawValidAnswer) {
			if (!(item instanceof Map<?, ?> mapItem)) {
				continue;
			}

			Boolean isValidAnswer = toBooleanValue(mapItem.get("isValidAnswer"));
			if (isValidAnswer == null) {
				continue;
			}

			Option target = null;
			Object idObj = mapItem.get("optionId");
			if (idObj != null && !String.valueOf(idObj).isBlank() && String.valueOf(idObj).trim().matches("\\d+")) {
				target = optionById.get(Integer.valueOf(String.valueOf(idObj).trim()));
			}
			if (target == null) {
				Object valueObj = mapItem.get("optionValue");
				if (valueObj == null) {
					valueObj = mapItem.get("optionsValue");
				}
				if (valueObj != null && !String.valueOf(valueObj).isBlank()) {
					target = optionByValue.get(String.valueOf(valueObj).trim().toLowerCase());
				}
			}

			if (target != null) {
				target.setIsActive(isValidAnswer);
				target.setUpdatedBy(user != null ? user.getEmployeeId() : null);
				target.setUpdatedDt(now);
				optionRepository.save(target);
			}
		}
	}

	private Boolean toBooleanValue(Object rawValue) {
		if (rawValue == null) {
			return null;
		}
		if (rawValue instanceof Boolean boolValue) {
			return boolValue;
		}
		String text = String.valueOf(rawValue).trim();
		if ("true".equalsIgnoreCase(text)) {
			return Boolean.TRUE;
		}
		if ("false".equalsIgnoreCase(text)) {
			return Boolean.FALSE;
		}
		return null;
	}

	private void applyDeletedOptionsFromRequest(List<Integer> deletedOptionIds, List<Option> allOptions, Employee user) {
		if (deletedOptionIds == null || deletedOptionIds.isEmpty() || allOptions == null || allOptions.isEmpty()) {
			return;
		}

		   Set<Integer> deletedSet = new java.util.HashSet<>(deletedOptionIds);
		   for (Option opt : allOptions) {
			   if (opt == null || opt.getOptionId() == 0) {
				   continue;
			   }
			   if (deletedSet.contains(opt.getOptionId())) {
				   optionRepository.delete(opt); // Hard delete from DB
			   }
		   }
	}

	private String removeDeletedIdsFromCsv(String existingCsv, List<Integer> deletedOptionIds) {
		if (existingCsv == null || existingCsv.isBlank() || deletedOptionIds == null || deletedOptionIds.isEmpty()) {
			return existingCsv;
		}

		Set<String> deleted = deletedOptionIds.stream().map(String::valueOf).collect(Collectors.toSet());
		List<String> kept = java.util.Arrays.stream(existingCsv.split(","))
				.map(String::trim)
				.filter(v -> !v.isBlank())
				.filter(v -> !deleted.contains(v))
				.collect(Collectors.toList());
		if (kept.isEmpty()) {
			return null;
		}
		return String.join(",", kept);
	}

	private List<Integer> removeDuplicateOptionsByValue(Integer questionId) {
		if (questionId == null) {
			return java.util.Collections.emptyList();
		}

		List<Option> all = optionRepository.findByQuestion_QuestionId(questionId);
		if (all == null || all.isEmpty()) {
			return java.util.Collections.emptyList();
		}

		Map<String, Option> keeperByValue = new HashMap<>();
		List<Integer> duplicateIds = new ArrayList<>();

		for (Option opt : all) {
			if (opt == null || opt.getOptionId() == 0) {
				continue;
			}
			String value = opt.getOptionValue();
			if (value == null || value.isBlank()) {
				continue;
			}
			String key = value.trim().toLowerCase();
			Option existing = keeperByValue.get(key);
			if (existing == null) {
				keeperByValue.put(key, opt);
				continue;
			}

			boolean currentActive = Boolean.TRUE.equals(opt.getIsActive());
			boolean existingActive = Boolean.TRUE.equals(existing.getIsActive());
			if (currentActive && !existingActive) {
				duplicateIds.add(existing.getOptionId());
				keeperByValue.put(key, opt);
			} else {
				duplicateIds.add(opt.getOptionId());
			}
		}

		for (Integer duplicateId : duplicateIds) {
			if (duplicateId == null) {
				continue;
			}
			for (Option opt : all) {
				if (opt != null && opt.getOptionId() == duplicateId) {
					optionRepository.delete(opt);
					break;
				}
			}
		}

		return duplicateIds;
	}
}
