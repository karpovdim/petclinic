/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner;

import java.util.List;
import java.util.Map;

import javax.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.visit.Visit;
import org.springframework.samples.petclinic.visit.VisitRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 * @author Dave Syer
 */
@Controller
@RequiredArgsConstructor
class VisitController {

	private final VisitRepository visitRepository;
	private final PetRepository petRepository;
	private final VetRepository vetRepository;
	private final OwnerRepository ownerRepository;


	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	/**
	 * Called before each and every @RequestMapping annotated method. 2 goals: - Make sure
	 * we always have fresh data - Since we do not use the session scope, make sure that
	 * Pet object always has an id (Even though id is not part of the form fields)
	 *
	 * @param petId
	 * @return Pet
	 */
	@ModelAttribute("visit")
	public Visit loadPetWithVisit(@PathVariable("petId") int petId, Map<String, Object> model) {
		Pet pet = this.petRepository.findById(petId).get();
		pet.setVisitsInternal(this.visitRepository.findByPetId(petId));
		model.put("pet", pet);
		Visit visit = new Visit();
		pet.addVisit(visit);
		return visit;
	}

	@ModelAttribute("vets")
	public List<Vet> loadVets() {
		return vetRepository.findAll();
	}

	// Spring MVC calls method loadPetWithVisit(...) before initNewVisitForm is called
	@GetMapping("/owners/*/pets/{petId}/visits/new")
	public String initNewVisitForm(@PathVariable("petId") int petId, Map<String, Object> model) {
		return "pets/createOrUpdateVisitForm";
	}

	// Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is called
	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String processNewVisitForm(@Valid Visit visit, @RequestParam int vetId, BindingResult result) {
		if (result.hasErrors()) {
			return "pets/createOrUpdateVisitForm";
		} else {
			Pet pet = petRepository.findById(visit.getPetId()).get();
			visit.setVet(vetRepository.findById(vetId).get());
			pet.getVisitsInternal().add(visit);
			this.visitRepository.save(visit);
			return "redirect:/owners/{ownerId}";
		}
	}

	@GetMapping("/owners/*/pets/{petId}/visits/{visitId}/edit")
	public String initUpdateForm(@PathVariable("visitId") int visitId, ModelMap model, @PathVariable int petId) {
		Visit visit = this.visitRepository.findById(visitId).get();
		model.put("visit", visit);
		return "/pets/createOrUpdateVisitForm";
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String processUpdateForm(@Valid Visit visit, BindingResult result, @RequestParam int vetId, ModelMap model, @PathVariable int visitId, @PathVariable int ownerId) {
		if (result.hasErrors()) {
			model.put("visit", visit);
			return "/pets/createOrUpdateVisitForm";
		} else {
			Pet pet = petRepository.findById(visit.getPetId()).get();
			visitRepository.deleteById(visitId);
			visit.setVet(vetRepository.findById(vetId).get());
			this.visitRepository.save(visit);
			return "redirect:/owners/{ownerId}";
		}
	}


	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/canceled")
	public String processCanceledVisit(Visit visit, ModelMap model,@PathVariable int visitId, @PathVariable int ownerId, BindingResult result) {
		if (result.hasErrors()) {
			model.put("visit", visit);
			return "/pets/createOrUpdateVisitForm";
		} else {
			Visit visitCanceled = visitRepository.findById(visitId).get();
			visitCanceled.setCanceled(!visitCanceled.getCanceled());
			visitRepository.save(visitCanceled);
			return "redirect:/owners/" + ownerId;
		}
	}
}
