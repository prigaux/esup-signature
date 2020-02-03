package org.esupportail.esupsignature.web.controller.user;

import org.apache.commons.io.IOUtils;
import org.esupportail.esupsignature.entity.Document;
import org.esupportail.esupsignature.entity.SignRequest;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.DocumentRepository;
import org.esupportail.esupsignature.repository.SignRequestRepository;
import org.esupportail.esupsignature.service.SignRequestService;
import org.esupportail.esupsignature.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequestMapping("/user/documents")
@Controller
@Transactional
@Scope(value="session")
public class DocumentController {
	
	private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);
	
	@Resource
	private UserService userService;

	@Resource
	private SignRequestRepository signRequestRepository;

	@Resource
	private DocumentRepository documentRepository;

	@Resource
	private SignRequestService signRequestService;

    @GetMapping(value = "/getfile/{id}")
	public ResponseEntity<Void> getFile(@PathVariable("id") Long id, HttpServletResponse response) throws IOException {
		User user = userService.getUserFromAuthentication();
		Document document = documentRepository.findById(id).get();
		if(document.equals(user.getKeystore())) {
			response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
			response.setContentType(document.getContentType());
			IOUtils.copy(document.getInputStream(), response.getOutputStream());
			return new ResponseEntity<>(HttpStatus.OK);
		}
		SignRequest signRequest = signRequestRepository.findById(document.getParentId()).get();
		if(signRequestService.checkUserViewRights(user, signRequest)) {
			response.setHeader("Content-Disposition", "inline;filename=\"" + document.getFileName() + "\"");
			response.setContentType(document.getContentType());
			IOUtils.copy(document.getInputStream(), response.getOutputStream());
			return new ResponseEntity<>(HttpStatus.OK);
		} else {
			logger.warn(user.getEppn() + " try to access " + id + " without view rights");
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
	}
}
