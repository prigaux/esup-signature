package org.esupportail.esupsignature.web.controller.admin;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.entity.enums.DocumentIOType;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.service.DocumentService;
import org.esupportail.esupsignature.service.FormService;
import org.esupportail.esupsignature.service.UserService;
import org.esupportail.esupsignature.service.WorkflowService;
import org.esupportail.esupsignature.service.pdf.PdfService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Controller
@RequestMapping("/admin/documents")
@Transactional
public class DocumentAdminController {
	
	private static final Logger logger = LoggerFactory.getLogger(DocumentAdminController.class);

	@Resource
	private DocumentService documentService;

	@Resource
	private DocumentRepository documentRepository;

	@Resource
	private UserService userService;

	@Resource
	private FormService formService;

	@Resource
	private WorkflowService workflowService;

	@Resource
	private PdfService pdfService;

	@ModelAttribute(value = "user", binding = false)
	public User getUser() {
		return userService.getCurrentUser();
	}

	@ModelAttribute(value = "authUser", binding = false)
	public User getAuthUser() {
		return userService.getUserFromAuthentication();
	}

	@ModelAttribute(value = "suUsers", binding = false)
	public List<User> getSuUsers(User authUser) {
		return userService.getSuUsers(authUser);
	}

	@ModelAttribute(value = "globalProperties")
	public GlobalProperties getGlobalProperties() {
		return this.globalProperties;
	}

	@Resource
	private GlobalProperties globalProperties;

	@GetMapping("/form")
	public String creatDocument(Model model) {
		model.addAttribute("document", new Document());
		return "admin/documents/create";
	}
	
	@GetMapping("/{id}")
	public String getDocumentById(@PathVariable("id") Long id, Model model) {
		Document document = documentRepository.findById(id).get();
		model.addAttribute("document", document);
		model.addAttribute("targetTypes", DocumentIOType.values());
		model.addAttribute("workflowTypes", workflowService.getClassesWorkflows());
		
		if(formService.getFormByDocument(document) != null) {
			model.addAttribute("form", formService.getFormByDocument(document));
		}
		return "admin/documents/show";
	}

//	@PostMapping("/{id}/generate")
//	public String generateForm(@PathVariable("id") Long id, String name, String title, String workflowType, String code, DocumentIOType targetType, String targetUri) throws IOException {
//		Document document = documentRepository.findById(id).get();
//		if(formService.getFormByDocument(document) == null) {
//			formService.createForm(document, name, title, workflowType, code, targetType, targetUri);
//		}
//		return "redirect:/admin/documents/" + id;
//	}
	
	@GetMapping("")
	public String getAllDocuments(Model model) {
		List<Document> documents = documentService.getAllDocuments();
		model.addAttribute("documents", documents);
		return "admin/documents/list";
	}
	
	@PostMapping("")
	public String addDocument(@RequestParam("multipartFile") MultipartFile multipartFile, RedirectAttributes redirectAttributes) throws IOException {
		Document document = documentService.createDocument(multipartFile.getInputStream(), multipartFile.getOriginalFilename(), multipartFile.getContentType());
		redirectAttributes.addFlashAttribute("info", "Document ajouté");
		return "redirect:/admin/documents/" + document.getId();
	}
	
	@DeleteMapping("/{id}")
	public String deleteDocument(@PathVariable("id") Long id) {
		Document document = documentRepository.findById(id).get();
		documentRepository.delete(document);
		return "redirect:/admin/documents";
	}

	@GetMapping("/{id}/getimagepdfpage/{page}")
	public ResponseEntity<Void> getImagePdfAsByteArray(@PathVariable("id") Long id, @PathVariable("page") int page, HttpServletResponse response) throws IOException {
		Document document = documentRepository.findById(id).get();
		InputStream in = null;
		try {
			in = pdfService.pageAsInputStream(document.getInputStream(), page);
		} catch (Exception e) {
			logger.error("page " + page + " not found in this document");
		}
		if(in == null) {
			try {
				in = pdfService.pageAsInputStream(document.getInputStream(), 0);
			} catch (Exception e) {
				logger.error("page " + page + " not found in this document");
			}
		}
	    response.setContentType(MediaType.IMAGE_PNG_VALUE);
	    IOUtils.copy(in, response.getOutputStream());
	    in.close();
	    return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@GetMapping(value = "documents/getfile/{id}")
	public ResponseEntity<Void> getFile(@PathVariable("id") Long id, HttpServletResponse response, Model model) {
		Document document = documentRepository.findById(id).get();
			try {
				response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
				response.setContentType(document.getContentType());
				IOUtils.copy(document.getInputStream(), response.getOutputStream());
				return new ResponseEntity<>(HttpStatus.OK);
			} catch (Exception e) {
				logger.error("get file error", e);
				return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
	}
}