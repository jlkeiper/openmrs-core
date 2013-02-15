/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.api.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Cohort;
import org.openmrs.Patient;
import org.openmrs.api.APIException;
import org.openmrs.api.CohortService;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.CohortDAO;
import org.openmrs.cohort.CohortDefinition;
import org.openmrs.cohort.CohortDefinitionItemHolder;
import org.openmrs.cohort.CohortDefinitionProvider;
import org.openmrs.report.EvaluationContext;
import org.openmrs.reporting.PatientCharacteristicFilter;
import org.openmrs.reporting.PatientSearch;
import org.openmrs.util.PrivilegeConstants;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * API functions related to Cohorts
 */
@Transactional
public class CohortServiceImpl extends BaseOpenmrsService implements CohortService {
	
	private Log log = LogFactory.getLog(this.getClass());
	
	private CohortDAO dao;
	
	private static Map<Class<? extends CohortDefinition>, CohortDefinitionProvider> cohortDefinitionProviders = null;
	
	/**
	 * @see org.openmrs.api.CohortService#setCohortDAO(org.openmrs.api.db.CohortDAO)
	 */
	public void setCohortDAO(CohortDAO dao) {
		this.dao = dao;
	}
	
	/**
	 * Clean up after this class. Set the static var to null so that the classloader can reclaim the
	 * space.
	 * 
	 * @see org.openmrs.api.impl.BaseOpenmrsService#onShutdown()
	 */
	public void onShutdown() {
		cohortDefinitionProviders = null;
	}
	
	/**
	 * @see org.openmrs.api.CohortService#saveCohort(org.openmrs.Cohort)
	 */
	public Cohort saveCohort(Cohort cohort) throws APIException {
		if (cohort.getCohortId() == null) {
			Context.requirePrivilege(PrivilegeConstants.ADD_COHORTS);
		} else {
			Context.requirePrivilege(PrivilegeConstants.EDIT_COHORTS);
		}
		if (cohort.getName() == null) {
			throw new APIException(Context.getMessageSourceService().getMessage("Cohort.save.nameRequired", null,
			    "Cohort name is required", Context.getLocale()));
		}
		if (cohort.getDescription() == null) {
			throw new APIException(Context.getMessageSourceService().getMessage("Cohort.save.descriptionRequired", null,
			    "Cohort description is required", Context.getLocale()));
		}
		if (log.isInfoEnabled())
			log.info("Saving cohort " + cohort);
		
		return dao.saveCohort(cohort);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#createCohort(org.openmrs.Cohort)
	 * @deprecated
	 */
	public Cohort createCohort(Cohort cohort) {
		return Context.getCohortService().saveCohort(cohort);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohort(java.lang.Integer)
	 */
	@Transactional(readOnly = true)
	public Cohort getCohort(Integer id) {
		return dao.getCohort(id);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohorts()
	 * @deprecated
	 */
	@Transactional(readOnly = true)
	public List<Cohort> getCohorts() {
		return getAllCohorts();
	}
	
	/**
	 * @see org.openmrs.api.CohortService#voidCohort(org.openmrs.Cohort, java.lang.String)
	 */
	public Cohort voidCohort(Cohort cohort, String reason) {
		// other setters done by the save handlers
		return saveCohort(cohort);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohortByUuid(java.lang.String)
	 */
	@Transactional(readOnly = true)
	public Cohort getCohortByUuid(String uuid) {
		return dao.getCohortByUuid(uuid);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#addPatientToCohort(org.openmrs.Cohort,
	 *      org.openmrs.Patient)
	 */
	public Cohort addPatientToCohort(Cohort cohort, Patient patient) {
		if (!cohort.contains(patient)) {
			cohort.getMemberIds().add(patient.getPatientId());
			saveCohort(cohort);
		}
		return cohort;
	}
	
	/**
	 * @see org.openmrs.api.CohortService#removePatientFromCohort(org.openmrs.Cohort,
	 *      org.openmrs.Patient)
	 */
	public Cohort removePatientFromCohort(Cohort cohort, Patient patient) {
		if (cohort.contains(patient)) {
			cohort.getMemberIds().remove(patient.getPatientId());
			saveCohort(cohort);
		}
		return cohort;
	}
	
	/**
	 * @see org.openmrs.api.CohortService#updateCohort(org.openmrs.Cohort)
	 * @deprecated
	 */
	public Cohort updateCohort(Cohort cohort) {
		return Context.getCohortService().saveCohort(cohort);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohortsContainingPatient(org.openmrs.Patient)
	 */
	@Transactional(readOnly = true)
	public List<Cohort> getCohortsContainingPatient(Patient patient) {
		return dao.getCohortsContainingPatientId(patient.getPatientId());
	}
	
	@Transactional(readOnly = true)
	public List<Cohort> getCohortsContainingPatientId(Integer patientId) {
		return dao.getCohortsContainingPatientId(patientId);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohorts(java.lang.String)
	 */
	@Transactional(readOnly = true)
	public List<Cohort> getCohorts(String nameFragment) throws APIException {
		return dao.getCohorts(nameFragment);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getAllCohorts()
	 */
	@Transactional(readOnly = true)
	public List<Cohort> getAllCohorts() throws APIException {
		return getAllCohorts(false);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getAllCohorts(boolean)
	 */
	@Transactional(readOnly = true)
	public List<Cohort> getAllCohorts(boolean includeVoided) throws APIException {
		return dao.getAllCohorts(includeVoided);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohort(java.lang.String)
	 */
	@Transactional(readOnly = true)
	public Cohort getCohort(String name) throws APIException {
		return dao.getCohort(name);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#purgeCohort(org.openmrs.Cohort)
	 */
	public Cohort purgeCohort(Cohort cohort) throws APIException {
		return dao.deleteCohort(cohort);
	}
	
	/**
	 * Auto generated method comment
	 * 
	 * @param definitionClass
	 * @return
	 * @deprecated see reportingcompatibility module
	 * @throws APIException
	 */
	@Deprecated
	@Transactional(readOnly = true)
	private CohortDefinitionProvider getCohortDefinitionProvider(Class<? extends CohortDefinition> definitionClass)
	        throws APIException {
		CohortDefinitionProvider ret = cohortDefinitionProviders.get(definitionClass);
		if (ret == null)
			throw new APIException("No CohortDefinitionProvider registered for " + definitionClass);
		else
			return ret;
	}
	
	/**
	 * @see org.openmrs.api.CohortService#evaluate(org.openmrs.cohort.CohortDefinition,
	 *      org.openmrs.report.EvaluationContext)
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	@Transactional(readOnly = true)
	public Cohort evaluate(CohortDefinition definition, EvaluationContext evalContext) throws APIException {
		CohortDefinitionProvider provider = getCohortDefinitionProvider(definition.getClass());
		return provider.evaluate(definition, evalContext);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getAllPatientsCohortDefinition()
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	@Transactional(readOnly = true)
	public CohortDefinition getAllPatientsCohortDefinition() {
		PatientSearch ps = new PatientSearch();
		ps.setFilterClass(PatientCharacteristicFilter.class);
		return ps;
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohortDefinition(java.lang.Class, java.lang.Integer)
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	@Transactional(readOnly = true)
	public CohortDefinition getCohortDefinition(Class<CohortDefinition> clazz, Integer id) {
		CohortDefinitionProvider provider = getCohortDefinitionProvider(clazz);
		return provider.getCohortDefinition(id);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohortDefinition(java.lang.String)
	 * @deprecated see reportingcompatibility module
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	@Transactional(readOnly = true)
	public CohortDefinition getCohortDefinition(String key) {
		try {
			
			String[] keyValues = key.split(":");
			Integer id = Integer.parseInt((keyValues[0] != null) ? keyValues[0] : "0");
			String className = (keyValues[1] != null) ? keyValues[1] : "";
			Class clazz = Class.forName(className);
			return getCohortDefinition(clazz, id);
		}
		catch (ClassNotFoundException e) {
			throw new APIException(e);
		}
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getAllCohortDefinitions()
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	@Transactional(readOnly = true)
	public List<CohortDefinitionItemHolder> getAllCohortDefinitions() {
		
		List<CohortDefinitionItemHolder> ret = new ArrayList<CohortDefinitionItemHolder>();
		for (CohortDefinitionProvider provider : cohortDefinitionProviders.values()) {
			
			log.info("Getting cohort definitions from " + provider.getClass());
			ret.addAll(provider.getAllCohortDefinitions());
		}
		return ret;
	}
	
	/**
	 * @see org.openmrs.api.CohortService#purgeCohortDefinition(org.openmrs.cohort.CohortDefinition)
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	public void purgeCohortDefinition(CohortDefinition definition) {
		CohortDefinitionProvider provider = getCohortDefinitionProvider(definition.getClass());
		provider.purgeCohortDefinition(definition);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#setCohortDefinitionProviders(Map)
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	public void setCohortDefinitionProviders(
	        Map<Class<? extends CohortDefinition>, CohortDefinitionProvider> providerClassMap) {
		for (Map.Entry<Class<? extends CohortDefinition>, CohortDefinitionProvider> entry : providerClassMap.entrySet()) {
			registerCohortDefinitionProvider(entry.getKey(), entry.getValue());
		}
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohortDefinitionProviders()
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	@Transactional(readOnly = true)
	public Map<Class<? extends CohortDefinition>, CohortDefinitionProvider> getCohortDefinitionProviders() {
		if (cohortDefinitionProviders == null)
			cohortDefinitionProviders = new LinkedHashMap<Class<? extends CohortDefinition>, CohortDefinitionProvider>();
		
		return cohortDefinitionProviders;
	}
	
	/**
	 * @see org.openmrs.api.CohortService#registerCohortDefinitionProvider(Class,
	 *      CohortDefinitionProvider)
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	@Transactional(readOnly = true)
	public void registerCohortDefinitionProvider(Class<? extends CohortDefinition> defClass,
	        CohortDefinitionProvider cohortDefProvider) throws APIException {
		getCohortDefinitionProviders().put(defClass, cohortDefProvider);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#removeCohortDefinitionProvider(java.lang.Class)
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	@Transactional(readOnly = true)
	public void removeCohortDefinitionProvider(Class<? extends CohortDefinitionProvider> providerClass) {
		
		// TODO: should this be looking through the values or the keys?
		for (Iterator<CohortDefinitionProvider> i = cohortDefinitionProviders.values().iterator(); i.hasNext();) {
			if (i.next().getClass().equals(providerClass))
				i.remove();
		}
	}
	
	/**
	 * @see org.openmrs.api.CohortService#saveCohortDefinition(org.openmrs.cohort.CohortDefinition)
	 * @deprecated see reportingcompatibility module
	 */
	@Deprecated
	public CohortDefinition saveCohortDefinition(CohortDefinition definition) throws APIException {
		CohortDefinitionProvider provider = getCohortDefinitionProvider(definition.getClass());
		return provider.saveCohortDefinition(definition);
	}
	
	/**
	 * @see org.openmrs.api.CohortService#getCohortDefinitions(java.lang.Class)
	 * @deprecated see reportingcompatibility module
	 */
	@SuppressWarnings("unchecked")
	@Deprecated
	@Transactional(readOnly = true)
	public List<CohortDefinitionItemHolder> getCohortDefinitions(Class providerClass) {
		CohortDefinitionProvider provider = getCohortDefinitionProvider(providerClass);
		return provider.getAllCohortDefinitions();
	}
}
