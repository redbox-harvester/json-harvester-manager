/*******************************************************************************
 *Copyright (C) 2014 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
 *
 *This program is free software: you can redistribute it and/or modify
 *it under the terms of the GNU General Public License as published by
 *the Free Software Foundation; either version 2 of the License, or
 *(at your option) any later version.
 *
 *This program is distributed in the hope that it will be useful,
 *but WITHOUT ANY WARRANTY; without even the implied warranty of
 *MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *GNU General Public License for more details.
 *
 *You should have received a copy of the GNU General Public License along
 *with this program; if not, write to the Free Software Foundation, Inc.,
 *51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 ******************************************************************************/
package au.com.redboxresearchdata.harvester.client.api.controller

import au.com.redboxresearchdata.harvester.client.*
import grails.test.spock.IntegrationSpec
import groovy.json.JsonSlurper
import spock.lang.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.plugins.testing.GrailsMockMultipartFile
import org.springframework.mock.web.MockMultipartHttpServletRequest
/**
 * 
 *  Integration tests for HarvesterController, the API controller.
 *  
 *  Note: The harvester client execution bits are intentionally skipped. This may change in the future.
 *  
 * @author Shilo Banihit
 *
 */
class HarvesterControllerSpec extends IntegrationSpec {
	
	def grailsApplication
	def harvesterManager
	def controller
	
	def setupSpec() {			
	}
	
	def cleanupSpec() {
		removeHarvesterClients()
	}
	
	def setup() {
		@Shared controller = new HarvesterController(harvesterManager:harvesterManager, grailsApplication:grailsApplication)
		controller.response.format = "json"
	}
	
	def cleanup() {		
	}	   

    def removeHarvesterClients() {
		harvesterManager.list().each {
			harvesterManager.remove(it)
		}
    }
	
	/* ---- Main stories ----*/
	void "There must be one at least one template in the configuration."() {
		@Shared controller = new HarvesterController(harvesterManager:harvesterManager, grailsApplication:grailsApplication)
		def jsonResponse = null
		def slurper = new JsonSlurper()
		when: "Listing the templates,"
			controller.listTemplates()
			jsonResponse = slurper.parseText(controller.response.contentAsString)
		then: "must return 'redboxSampleJdbcHarvester'"
			jsonResponse[0] == "redboxSampleJdbcHarvester"		
	}
	
	void "A template's description and location URL can be retrieved."() {
		@Shared controller = new HarvesterController(harvesterManager:harvesterManager, grailsApplication:grailsApplication)			
		controller.params.id = "redboxSampleJdbcHarvester"
		when: "Retrieving the template location,"
			controller.getTemplateLocation()			
		then: "must return a non-empty value."
			controller.response.contentAsString.length() > 0
		when: "Retrieving the template description,"
			controller.response.reset()
			controller.getTemplateDescription()
		then: "must return a non-empty value."
			controller.response.contentAsString.length() > 0
	}

    void "Even with no harvesters, the index should produce an empty list."() {
		setup: "With no harvesters,"
			@Shared controller = new HarvesterController(harvesterManager:harvesterManager, grailsApplication:grailsApplication)
		when: "an index request,"	
			controller.index()
		then: "and must return an empty response."
			controller.response.text == "[]"										 	
    }
	
	void "Adding a new harvester from template should succeed, must appear in the harvester listing, can be packaged, and can be deleted."() {
		setup: "With no harvesters and providing a harvester id and template name,"
			@Shared controller = new HarvesterController(harvesterManager:harvesterManager, grailsApplication:grailsApplication)
			def harvesterId = "testRedboxSampleJdbcHarvester"			 		
			controller.params.id = harvesterId
			controller.params.template = "redboxSampleJdbcHarvester"
			def jsonResponse = null			
			def slurper = new JsonSlurper()			
		when: "issuing an createFromTemplate command"
			controller.createFromTemplate()
			jsonResponse = slurper.parseText(controller.response.contentAsString) 
		then: "should be successful"						
			jsonResponse.success == true
			jsonResponse.message == "Added new harvester:${harvesterId}" 		
		when: "and a listing"			
			controller.response.reset()
			controller.index()
			jsonResponse = slurper.parseText(controller.response.contentAsString)
		then: "should return the newly added harvester."
			jsonResponse[0] == harvesterId
		when: "When harvester is packaged without a destination set, "
			controller.response.reset()			
			controller.pack()
		then: "it should succeed."
			controller.response.status == 200
			controller.response.contentAsByteArray.length > 0
			controller.response.containsHeader("Content-disposition")
			controller.response.getHeader("Content-disposition").indexOf(harvesterId) > 0
		when: "When harvester is packaged with a destination set, "
			controller.response.reset()
			controller.params.destFileName = harvesterId + "-custompackage.zip" 			
			controller.pack()
		then: "it should succeed."
			controller.response.status == 200
			controller.response.contentAsByteArray.length > 0
			controller.response.containsHeader("Content-disposition")
			controller.response.getHeader("Content-disposition").indexOf(controller.params.destFileName) > 0
		when: "When the harvester is deleted"
			controller.response.reset()
			controller.remove()
			jsonResponse = slurper.parseText(controller.response.contentAsString)				
		then: "it should succeed,"
			jsonResponse.success == true
			jsonResponse.message == "Removed harvester:${harvesterId}"
		when: "and the harvester list"		
			controller.response.reset()
			controller.index()
			jsonResponse = slurper.parseText(controller.response.contentAsString)
		then: "must be empty."
			jsonResponse.size() == 0			
	}
	
	void "Uploading a package should add the new harvester, it must appear on the harvester list, and it can be deleted."() {
		def controller = new HarvesterController(harvesterManager:harvesterManager, grailsApplication:grailsApplication)
		controller.metaClass.request = new MockMultipartHttpServletRequest()
		controller.response.format = "json"
		def harvesterId = "testRedboxSampleJdbcHarvester-custompackage"
		def jsonResponse = null
		def slurper = new JsonSlurper()
		controller.params.id = harvesterId				
		when: "Uploading a package,"					
			final harvesterPackage = new GrailsMockMultipartFile("harvesterPackage", harvesterId + ".zip", "application/zip", "test".getBytes())
			controller.request.addFile(harvesterPackage)
			controller.upload()
			jsonResponse = slurper.parseText(controller.response.contentAsString)
		then: "should succeed."
			controller.response.status == 200			
			jsonResponse.success == true
			jsonResponse.message == "Added new harvester:${harvesterId}"
		when: "Harvester listing"
			controller.response.reset()
			controller.index()
			jsonResponse = slurper.parseText(controller.response.contentAsString)
		then: "must include the new harvester."
			jsonResponse[0] == harvesterId
		when:"Deleting the harvester"
			controller.response.reset()
			controller.remove()
			jsonResponse = slurper.parseText(controller.response.contentAsString)
		then: "should succeed,"
			jsonResponse.success == true
			jsonResponse.message == "Removed harvester:${harvesterId}"
		when: "and the harvester list"
			controller.response.reset()
			controller.index()
			jsonResponse = slurper.parseText(controller.response.contentAsString)
		then: "must be empty."
			jsonResponse.size() == 0		
	}	
	
}
