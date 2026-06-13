CREATE TABLE compliance.employee_question_mapping
(
	employee_question_mapping_id  serial not null,
	employee_id INTEGER NOT NULL,
    question_id INTEGER NOT NULL,
	is_active             BOOLEAN DEFAULT TRUE,
    created_by            INTEGER,
    created_dt            TIMESTAMPTZ DEFAULT NOW(),
    updated_by            INTEGER,
    updated_dt            TIMESTAMPTZ DEFAULT NOW(),
	

    CONSTRAINT pk_employee_question_mapping 
        PRIMARY KEY (employee_id, question_id),

    CONSTRAINT fk_eqm_employee 
        FOREIGN KEY (employee_id)
        REFERENCES base.employee (employee_id),

    CONSTRAINT fk_eqm_question 
        FOREIGN KEY (question_id)
        REFERENCES compliance.question_bank (question_id)
);
