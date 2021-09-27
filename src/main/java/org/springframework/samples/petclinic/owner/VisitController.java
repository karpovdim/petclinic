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

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

	public static final String PETS_CREATE_OR_UPDATE_VISIT_FORM = "/pets/createOrUpdateVisitForm";
	public static final String REDIRECT = "redirect:";
	public static final String OWNERS_OWNER_ID = "/owners/{ownerId}";
	private final VisitRepository visitRepository;
	private final PetRepository petRepository;
	private final VetRepository vetRepository;


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
		Pet pet = this.petRepository.findById(petId).orElseThrow();;
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
		return PETS_CREATE_OR_UPDATE_VISIT_FORM;
	}

	// Spring MVC calls method loadPetWithVisit(...) before processNewVisitForm is called
	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/new")
	public String processNewVisitForm(@Valid Visit visit, @RequestParam int vetId, BindingResult result) {
		if (result.hasErrors()) {
			return PETS_CREATE_OR_UPDATE_VISIT_FORM;
		} else {
			Pet pet = petRepository.findById(visit.getPetId()).orElseThrow();
			visit.setVet(vetRepository.findById(vetId).orElseThrow());
			pet.getVisitsInternal().add(visit);
			this.visitRepository.save(visit);
			return REDIRECT + OWNERS_OWNER_ID;
		}
	}

	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String initUpdateForm(@PathVariable("visitId") int visitId, ModelMap model, @PathVariable int ownerId) {
		Set<Pet> pets = petRepository.findAllByOwnerId(ownerId);
		Visit visit = this.visitRepository.findById(visitId).orElseThrow();
		model.put("visit", visit);
		model.put("pets", pets);
		return PETS_CREATE_OR_UPDATE_VISIT_FORM;
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/edit")
	public String processUpdateForm(@Valid Visit visit, BindingResult result,
									@RequestParam int vetId, ModelMap model,
									@PathVariable int visitId, @RequestParam int petId) {
		if (result.hasErrors()) {
			model.put("visit", visit);
			return PETS_CREATE_OR_UPDATE_VISIT_FORM;
		} else {
			visitRepository.deleteById(visitId);
			visit.setVet(vetRepository.findById(vetId).orElseThrow());
			visit.setPetId(petId);
			this.visitRepository.save(visit);
			return REDIRECT + OWNERS_OWNER_ID;
		}
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}/canceled")
	public String processCanceledVisit(Visit visit, ModelMap model, @PathVariable int visitId, BindingResult result) {
		if (result.hasErrors()) {
			model.put("visit", visit);
			return PETS_CREATE_OR_UPDATE_VISIT_FORM;
		} else {
			Visit visitCanceled = visitRepository.findById(visitId).orElseThrow();
			visitCanceled.setCanceled(!visitCanceled.getCanceled());
			visitRepository.save(visitCanceled);
			return REDIRECT + OWNERS_OWNER_ID;
		}
	}
}
