package org.esupportail.esupsignature.service.workflow.impl;

import org.esupportail.esupsignature.entity.Data;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.WorkflowStep;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.service.RecipientService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.workflow.DefaultWorkflow;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Component
public class CreatorAndTwoStepsWorkflow extends DefaultWorkflow {

	private String name = "CreatorAndTwoStepsWorkflow";
	private String description = "Signature du créateur puis de deux signataires en série";
	private List<WorkflowStep> workflowSteps;

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public List<WorkflowStep> getWorkflowSteps() {
		if(this.workflowSteps == null) {
			try {
				this.workflowSteps = generateWorkflowSteps(userService.getCreatorUser(), null, null);
			} catch (EsupSignatureUserException e) {
				return null;
			}
		}
		return this.workflowSteps;
	}

	public void initWorkflowSteps() {
		this.workflowSteps = new ArrayList<>();
	}

	@Override
	public List<WorkflowStep> generateWorkflowSteps(User user, Data data, List<String> recipentEmailsStep) throws EsupSignatureUserException {
		List<WorkflowStep> workflowSteps = new ArrayList<>();
		//STEP 1
		WorkflowStep workflowStep1 = new WorkflowStep();
		workflowStep1.setStepNumber(1);
		workflowStep1.getRecipients().add(recipientService.createRecipient(null, user));
		workflowSteps.add(workflowStep1);
		//STEP 2
		WorkflowStep workflowStep2 = new WorkflowStep();
		workflowStep2.setStepNumber(2);
		if(data != null) {
			workflowStep2.setRecipients(workflowService.getFavoriteRecipientEmail(2, data.getForm(), recipentEmailsStep, user));
		} else {
			workflowStep2.getRecipients().add(recipientService.createRecipient(null, userService.getGenericUser("Utilisateur issue des favoris", "")));
		}
		workflowStep2.setChangeable(true);
		workflowSteps.add(workflowStep2);
		//STEP 3
		WorkflowStep workflowStep3 = new WorkflowStep();
		workflowStep3.setStepNumber(3);
		if(data != null) {
			workflowStep3.setRecipients(workflowService.getFavoriteRecipientEmail(3, data.getForm(), recipentEmailsStep, user));
		} else {
			workflowStep3.getRecipients().add(recipientService.createRecipient(null, userService.getGenericUser("Utilisateur issue des favoris", "")));
		}
		workflowStep3.setChangeable(true);
		workflowSteps.add(workflowStep3);
		return workflowSteps;
	}
}

