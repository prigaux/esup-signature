package org.esupportail.esupsignature.service;

import org.esupportail.esupsignature.entity.*;
import org.esupportail.esupsignature.entity.SignBook.SignBookType;
import org.esupportail.esupsignature.entity.enums.SignRequestStatus;
import org.esupportail.esupsignature.entity.enums.SignType;
import org.esupportail.esupsignature.exception.EsupSignatureException;
import org.esupportail.esupsignature.exception.EsupSignatureUserException;
import org.esupportail.esupsignature.repository.*;
import org.esupportail.esupsignature.service.mail.MailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class SignBookService {

    private static final Logger logger = LoggerFactory.getLogger(SignBookService.class);

    @Resource
    private SignBookRepository signBookRepository;

    @Resource
    private SignRequestService signRequestService;

    @Resource
    private RecipientService recipientService;

    @Resource
    private DataRepository dataRepository;

    @Resource
    private WorkflowStepRepository workflowStepRepository;

    @Resource
    private UserService userService;

    @Resource
    private MailService mailService;

    @Resource
    private LogRepository logRepository;

    @Resource
    private WorkflowService workflowService;

    @Resource
    private WorkflowService workflowStepService;

    @Resource
    private RecipientRepository recipientRepository;

    public List<SignBook> getAllSignBooks() {
        List<SignBook> list = new ArrayList<>();
        signBookRepository.findAll().forEach(e -> list.add(e));
        return list;
    }

    public SignBook createSignBook(String name, User user, boolean external) throws EsupSignatureException {
        name = generateName(name, user);
        if (signBookRepository.countByName(name) == 0) {
            SignBook signBook = new SignBook();
            signBook.setStatus(SignRequestStatus.draft);
            signBook.setName(name);
            signBook.setCreateBy(user);
            signBook.setCreateDate(new Date());
            signBook.setExternal(external);
            signBookRepository.save(signBook);
            return signBook;
        } else {
            return signBookRepository.findByName(name).get(0);
//            throw new EsupSignatureException("Un circuit porte déjà le nom : " + name);
        }
    }

    public SignBook getSignBook(String name, User user) throws EsupSignatureException {
        if (signBookRepository.countByName(name) == 0) {
            logger.info("create new signBook : " + name);
            return createSignBook(name, user, false);
        } else {
            return signBookRepository.findByName(name).get(0);
        }
    }

    public void addSignRequest(SignBook signBook, SignRequest signRequest) {
        signBook.getSignRequests().add(signRequest);
        signRequest.setParentSignBook(signBook);
    }

    public void delete(SignBook signBook) {
        List<Data> datas = dataRepository.findBySignBook(signBook);
        for (Data data : datas) {
            data.setSignBook(null);
            dataRepository.save(data);
        }
        signBookRepository.delete(signBook);
    }

    public void removeSignRequestFromSignBook(SignBook signBook, SignRequest signRequest) {
        signBook.getSignRequests().remove(signRequest);
    }

    public boolean checkUserManageRights(User user, SignBook signBook) {
        if (signBook.getCreateBy().equals(user) || signBook.getCreateBy().getEppn().equals("System")) {
            return true;
        } else {
            return false;
        }
    }

    public boolean preAuthorizeView(Long id, User user) {
        SignBook signBook = signBookRepository.findById(id).get();
        return checkUserViewRights(user, signBook);
    }

    public boolean preAuthorizeManage(Long id, User user) {
        SignBook signBook = signBookRepository.findById(id).get();
        return checkUserManageRights(user, signBook);
    }

    public boolean preAuthorizeManage(String name, User user) throws EsupSignatureException {
        SignBook signBook = getSignBook(name, user);
        return checkUserManageRights(user, signBook);
    }

    public void importWorkflow(SignBook signBook, Workflow workflow){
        logger.info("import workflow steps in signBook " + signBook.getName() + " - " +signBook.getId());
        for (WorkflowStep workflowStep : workflow.getWorkflowSteps()) {
            List<String> recipientEmails = new ArrayList<>();
            for(Recipient recipient : workflowStep.getRecipients()) {
                recipientEmails.add(recipient.getUser().getEmail());
            }
            WorkflowStep newWorkflowStep = null;
            try {
                newWorkflowStep = workflowService.createWorkflowStep("", "signBook", signBook.getId(), workflowStep.getAllSignToComplete(), workflowStep.getSignType(), recipientEmails.toArray(String[]::new));
            } catch (EsupSignatureUserException e) {
                logger.error("error on import workflow", e);
            }
            signBook.getWorkflowSteps().add(newWorkflowStep);
        }
        signBook.setTargetType(workflow.getTargetType());
        signBook.setDocumentsTargetUri(workflow.getDocumentsTargetUri());
    }

    public void saveWorkflow(String name, User user, SignBook signBook) throws EsupSignatureException {
        Workflow workflow = workflowService.createWorkflow(name, user, false);
        workflow.setDescription(name);
        for(WorkflowStep workflowStep : signBook.getWorkflowSteps()) {
            List<String> recipientsEmails = new ArrayList<>();
            for (Recipient recipient : workflowStep.getRecipients()) {
                recipientsEmails.add(recipient.getUser().getEmail());
            }
            WorkflowStep toSaveWorkflowStep = null;
            try {
                toSaveWorkflowStep = workflowService.createWorkflowStep("", "signBook", signBook.getId(), workflowStep.getAllSignToComplete(), workflowStep.getSignType(), recipientsEmails.toArray(String[]::new));
            } catch (EsupSignatureUserException e) {
                logger.error("error on save workflow", e);
            }
            workflow.getWorkflowSteps().add(toSaveWorkflowStep);
        }
    }

    public void completeSignBook(SignBook signBook, User user) throws EsupSignatureException {
        updateStatus(signBook, SignRequestStatus.completed, "Tous les documents sont signés", "SUCCESS", "");
        signRequestService.completeSignRequests(signBook.getSignRequests(), signBook.getTargetType(), signBook.getDocumentsTargetUri(), user);
    }

    public void exportFilesToTarget(SignBook signBook) throws EsupSignatureException {
        logger.trace("export signRequest to : " + signBook.getTargetType() + "://" + signBook.getDocumentsTargetUri());
        if (signBook.getStatus().equals(SignRequestStatus.completed)) {
            signRequestService.sendSignRequestsToTarget(signBook.getName(), signBook.getSignRequests(), signBook.getTargetType(), signBook.getDocumentsTargetUri());
            signBook.setStatus(SignRequestStatus.exported);
        }
    }

    public void removeStep(SignBook signBook, int step) {
        WorkflowStep workflowStep = signBook.getWorkflowSteps().get(step);
        signBook.getWorkflowSteps().remove(step);
        workflowStepRepository.delete(workflowStep);
    }

    public void removeStepRecipient(SignBook signBook, int step, Long recipientId) {
        Recipient recipientToRemove = recipientRepository.findById(recipientId).get();
        WorkflowStep workflowStep = signBook.getWorkflowSteps().get(step);
        workflowStep.getRecipients().remove(recipientToRemove);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            if(signRequest.getCurrentStepNumber() == step + 1) {
                for(Recipient recipient : signRequest.getRecipients()) {
                    if(recipient.getUser().equals(recipientToRemove.getUser())) {
                        signRequest.getRecipients().remove(recipient);
                    }
                }
            }
        }
    }

    public void toggleNeedAllSign(SignBook signBook, int step, Boolean allSignToComplete) {
        WorkflowStep workflowStep = signBook.getWorkflowSteps().get(step);
        if(allSignToComplete != null && !allSignToComplete.equals(workflowStep.getAllSignToComplete())) {
            workflowService.toggleAllSignToCompleteForWorkflowStep(workflowStep);
        }
    }

    public void changeSignType(SignBook signBook, int step, SignType signType) {
        WorkflowStep workflowStep = signBook.getWorkflowSteps().get(step);
        workflowService.setSignTypeForWorkflowStep(signType, workflowStep);
    }

    public boolean isUserSignAllDocs(SignBook signBook, User user) {
        for (SignRequest signRequest : signBook.getSignRequests()) {
            if (recipientService.needSign(signRequest.getRecipients(), user)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStepAllDocsDone(SignBook signBook) {
        for (SignRequest signRequest : signBook.getSignRequests()) {
            if (signRequest.getStatus().equals(SignRequestStatus.pending)) {
                return false;
            }
        }
        return true;
    }

    public boolean isStepAllSignDone(SignBook signBook) {
        WorkflowStep currentWorkflowStep = getCurrentWorkflowStep(signBook);
        if (currentWorkflowStep.getAllSignToComplete() && !workflowStepService.isWorkflowStepFullSigned(currentWorkflowStep)) {
            return false;
        }
        return true;
    }

    public boolean nextWorkFlowStep(SignBook signBook) {
        signBook.setCurrentWorkflowStepNumber(signBook.getCurrentWorkflowStepNumber() + 1);
        if(signBook.getWorkflowSteps().size() >= signBook.getCurrentWorkflowStepNumber()) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isNextWorkFlowStep(SignBook signBook) {
        if(signBook.getWorkflowSteps().size() >= signBook.getCurrentWorkflowStepNumber() + 1) {
            return true;
        } else {
            return false;
        }
    }

    public boolean checkUserViewRights(User user, SignBook signBook) {
        if(signBook.getCreateBy().equals(user) || (getCurrentWorkflowStep(signBook) != null && recipientService.recipientsContainsUser(getCurrentWorkflowStep(signBook).getRecipients(), user) > 0)) {
            return true;
        }
        return false;
    }

    public void pendingSignBook(SignBook signBook, User user) {
        WorkflowStep currentWorkflowStep = getCurrentWorkflowStep(signBook);
        updateStatus(signBook, SignRequestStatus.pending, "Circuit envoyé pour signature de l'étape " + signBook.getCurrentWorkflowStepNumber(), "SUCCESS", signBook.getComment());
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequestService.addRecipients(signRequest, currentWorkflowStep.getRecipients());
            signRequestService.pendingSignRequest(signRequest, currentWorkflowStep.getSignType(), currentWorkflowStep.getAllSignToComplete(), user);
        }

// TODO : verifier doublon
//        for (Recipient recipient : currentWorkflowStep.getRecipients()) {
//            User recipientUser = recipient.getUser();
//            if (recipientUser.getEmailAlertFrequency() == null || recipientUser.getEmailAlertFrequency().equals(User.EmailAlertFrequency.immediately) || userService.checkEmailAlert(recipientUser)) {
//                if(!recipientUser.equals(user)) {
//                    userService.sendEmailAlert(recipientUser);
//                }
//            }
//        }
        logger.info("Circuit " + signBook.getId() + " envoyé pour signature de l'étape " + signBook.getCurrentWorkflowStepNumber());
    }

    public void updateStatus(SignBook signBook, SignRequestStatus signRequestStatus, String action, String returnCode, String comment) {
        Log log = new Log();
        log.setSignRequestId(signBook.getId());
        log.setEppn(userService.getUserFromAuthentication().getEppn());
        log.setEppnFor(userService.getSuEppn());
        log.setIp(userService.getUserFromAuthentication().getIp());
        log.setInitialStatus(signBook.getStatus().toString());
        log.setLogDate(new Date());
        log.setAction(action);
        log.setReturnCode(returnCode);
        log.setComment(comment);
        if(signRequestStatus != null) {
            log.setFinalStatus(signRequestStatus.toString());
            signBook.setStatus(signRequestStatus);
        } else {
            log.setFinalStatus(signBook.getStatus().toString());
        }
        logRepository.save(log);
    }

    public WorkflowStep getCurrentWorkflowStep(SignBook signBook) {
        if(signBook.getCurrentWorkflowStepNumber() > 0 && signBook.getWorkflowSteps().size() >= signBook.getCurrentWorkflowStepNumber()) {
            return signBook.getWorkflowSteps().get(signBook.getCurrentWorkflowStepNumber() - 1);
        } else {
            return null;
        }
    }

    public void refuse(SignBook signBook, String comment, User user) {
        mailService.sendRefusedMail(signBook, comment);
        updateStatus(signBook, SignRequestStatus.refused, "Un des documents du a été refusé, ceci annule toute la procédure", "SUCCESS", comment);
        for(SignRequest signRequest : signBook.getSignRequests()) {
            signRequest.setComment(comment);
            signRequestService.updateStatus(signRequest, SignRequestStatus.refused, "Refusé", "SUCCESS", null, null, null);
            for(Recipient recipient : getCurrentWorkflowStep(signBook).getRecipients()) {
                if(recipient.getUser().equals(user)) {
                    recipient.setSigned(true);
                }
            }
        }
    }

    private String generateName(String name, User user) {
        String signBookName = "";
        signBookName += user.getFirstname().substring(0, 1).toUpperCase();
        signBookName += user.getName().substring(0, 1).toUpperCase();
        signBookName += "_";
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        signBookName += format.format(new Date());
        if(!name.isEmpty()) {
            signBookName += "_";
            signBookName += name.replaceAll("[\\\\/:*?\"<>|]", "-");
        }
        return signBookName;
    }
}
