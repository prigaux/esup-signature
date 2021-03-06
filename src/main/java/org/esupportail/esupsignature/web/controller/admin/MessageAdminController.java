/**
 * Licensed to ESUP-Portail under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 *
 * ESUP-Portail licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.esupportail.esupsignature.web.controller.admin;

import org.esupportail.esupsignature.config.GlobalProperties;
import org.esupportail.esupsignature.entity.Message;
import org.esupportail.esupsignature.entity.User;
import org.esupportail.esupsignature.repository.MessageRepository;
import org.esupportail.esupsignature.service.UserService;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RequestMapping("/admin/messages")
@Controller
public class MessageAdminController {

	@ModelAttribute("active")
	public String getActiveMenu() {
		return "admin";
	}

	@ModelAttribute(value = "user", binding = false)
	public User getUser() {
		return userService.getCurrentUser();
	}

	@ModelAttribute(value = "authUser", binding = false)
	public User getAuthUser() {
		return userService.getUserFromAuthentication();
	}

	@ModelAttribute(value = "globalProperties")
	public GlobalProperties getGlobalProperties() {
		return this.globalProperties;
	}

	@Resource
	private GlobalProperties globalProperties;

	@Resource
	private MessageRepository messageRepository;

	@Resource
	private UserService userService;

	@GetMapping
	public String messages(Pageable pageable, Model model) {
		model.addAttribute("messages", messageRepository.findAll(pageable));
		return "admin/messages";
	}

	@PostMapping("/add")
	public String addMessage(@RequestParam String text, @RequestParam String endDate) throws ParseException {
		Message message = new Message();
		Date date = new SimpleDateFormat("yyyy-MM-dd").parse(endDate);
		message.setEndDate(date);
		message.setText(text);
		messageRepository.save(message);
		return "redirect:/admin/messages";
	}

	@DeleteMapping("{id}")
	public String messages(@PathVariable Long id, RedirectAttributes redirectAttributes) {
		messageRepository.deleteById(id);
		redirectAttributes.addFlashAttribute("messageError", "Message supprimé");
		return "redirect:/admin/messages";
	}

}
