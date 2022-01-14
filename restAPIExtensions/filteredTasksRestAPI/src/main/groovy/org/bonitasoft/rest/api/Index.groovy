package org.bonitasoft.rest.api;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

import org.apache.commons.lang3.StringUtils
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor
import org.bonitasoft.engine.search.SearchOptionsBuilder
import org.bonitasoft.engine.search.SearchResult
import org.bonitasoft.web.extension.ResourceProvider
import org.bonitasoft.web.extension.rest.RestAPIContext
import org.bonitasoft.web.extension.rest.RestApiController
import org.bonitasoft.web.extension.rest.RestApiResponse
import org.bonitasoft.web.extension.rest.RestApiResponseBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.company.model.Exemple
import com.company.model.ExempleDAO

import groovy.json.JsonBuilder

class Index implements RestApiController {

    private static final Logger LOGGER = LoggerFactory.getLogger(Index.class)

    @Override
    RestApiResponse doHandle(HttpServletRequest request, RestApiResponseBuilder responseBuilder, RestAPIContext context) {
 
        def site = request.getParameter "site"
        def auteur = request.getParameter "auteur"
		
		site = StringUtils.isBlank(site) ? null : site+"%";
		auteur = StringUtils.isBlank(auteur) ? null : auteur+"%";
		
		SearchOptionsBuilder searchOptions = new SearchOptionsBuilder(0, Integer.MAX_VALUE);
		searchOptions.filter(HumanTaskInstanceSearchDescriptor.ASSIGNEE_ID, context.getApiSession().getUserId()).or().filter(HumanTaskInstanceSearchDescriptor.ASSIGNEE_ID, 0);
		long processDefId = context.apiClient.processAPI.getLatestProcessDefinitionId("Pool");
		SearchResult<HumanTaskInstance> humanTaskLists = context.apiClient.processAPI.searchAssignedAndPendingHumanTasks(processDefId, searchOptions.done());
		HashMap<Long, HumanTaskInstance> caseTaskMap = new HashMap<>();

		for(HumanTaskInstance hti : humanTaskLists.getResult()) {
			caseTaskMap.put(hti.parentProcessInstanceId, hti);
		}
		
		ExempleDAO exempleDAO = context.apiClient.getDAO(ExempleDAO.class);
		Map<Long, Object> result = new HashMap<Long, Object>();
		List<Exemple> exemples = exempleDAO.customQuery(caseTaskMap.keySet().toArray(new Long[0]), site, auteur, 0, Integer.MAX_VALUE);
		
		for(Exemple ex : exemples) {
			HumanTaskInstance hti = caseTaskMap.get(ex.caseId);
			result.put(hti.id, ["humantask": hti, "exemple": ex]);
		}

        return buildResponse(responseBuilder, HttpServletResponse.SC_OK, new JsonBuilder(result).toString())
    }

    /**
     * Build an HTTP response.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  httpStatus the status of the response
     * @param  body the response body
     * @return a RestAPIResponse
     */
    RestApiResponse buildResponse(RestApiResponseBuilder responseBuilder, int httpStatus, Serializable body) {
        return responseBuilder.with {
            withResponseStatus(httpStatus)
            withResponse(body)
            build()
        }
    }

    /**
     * Returns a paged result like Bonita BPM REST APIs.
     * Build a response with a content-range.
     *
     * @param  responseBuilder the Rest API response builder
     * @param  body the response body
     * @param  p the page index
     * @param  c the number of result per page
     * @param  total the total number of results
     * @return a RestAPIResponse
     */
    RestApiResponse buildPagedResponse(RestApiResponseBuilder responseBuilder, Serializable body, int p, int c, long total) {
        return responseBuilder.with {
            withContentRange(p,c,total)
            withResponse(body)
            build()
        }
    }

    /**
     * Load a property file into a java.util.Properties
     */
    Properties loadProperties(String fileName, ResourceProvider resourceProvider) {
        Properties props = new Properties()
        resourceProvider.getResourceAsStream(fileName).withStream { InputStream s ->
            props.load s
        }
        props
    }

}
