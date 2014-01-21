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

/**
 * Facade over the HarvesterManager class.
 * 
 * @author Shilo Banihit
 *
 */
class HarvesterController {
	def harvesterManager
	def grailsApplication
	
    def index() {
		def harvesters = harvesterManager.list() 
		respond harvesters
	}
	
	/**
	 * URL: <app-url>/harvester/remove/<harvester-id>
	 * 
	 * @return JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def remove() {				
		respond harvesterManager.remove(params.id)		
	}
	
	/**
	 * URL: <app-url>/harvester/createFromTemplate/<harvester-id>
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def createFromTemplate() {
		respond harvesterManager.createFromTemplate(params.id, params.template)
	}
	
	/**
	 * URL: <app-url>/harvester/add/<harvester-id>?configPath=<optional config path of the package>&packagePath=<optional custom package path>
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def add() {
		respond harvesterManager.add(params.id, params.configPath, params.packagePath)
	}
	
	/**
	 * URL: <app-url>/harvester/listTemplates
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def listTemplates() {
		respond harvesterManager.listTemplates()
	}
	
	/**
	 * URL: <app-url>/harvester/getTemplateLocation/<harvester id>
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Template Location>"}
	 */
	def getTemplateLocation() {		
		respond harvesterManager.getTemplateLocation(params.id)
	}
	
	/**
	 * URL: <app-url>/harvester/getTemplateDescription/<harvester id>
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Template Description>"}
	 */
	def getTemplateDescription() {		
		respond harvesterManager.getTemplateDescription(params.id)
	}
	
	/**
	 * URL: <app-url>/harvester/startAll
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def startAll() {
		def status = [success:true, message:"All started"]
		harvesterManager.startAll()
		respond status
	}
	
	/**
	 * URL: <app-url>/harvester/stopAll
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def stopAll() {
		def status = [success:true, message:"All stopped"]
		harvesterManager.stopAll()
		respond status
	}
	
	/**
	 * URL: <app-url>/harvester/start/<harvester id>
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def start() {				
		respond harvesterManager.start(params.id)
	}
	
	/**
	 * URL: <app-url>/harvester/stop/<harvester id>
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def stop() {				
		respond harvesterManager.stop(params.id)
	}
	
	/**
	 * URL: <app-url>/harvester/pack/<harvester id>?destFileName=<optional value, the local destination file name, must be a child of 'harvest.base' config value>
	 *
	 * @return Success: Package file as attachment, with set filename header/
	 *         Failure: JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def pack() {
		def packStat = harvesterManager.pack(params.id, params.destFileName)
		if (packStat.success) {
			def harvesterPackage = new File(packStat.message)
			response.setContentType("application/octet-stream")
			response.setHeader("Content-disposition", "attachment;filename=${harvesterPackage.getName()}")
			def is = harvesterPackage.newInputStream()
			response.outputStream << is
			is.close() 
		} else {
			respond packStat
		}
	}
	
	/**
	 * URL: <app-url>/harvester/upload/<harvester id>?harvesterPackage=<package file of harvester>&configPath=<optional, config file within the package>
	 *
	 * @return JSON: {"success":<boolean>, "message":"<Message>"}
	 */
	def upload() {
		def harvesterPackage = request.getFile('harvesterPackage')
		if (harvesterPackage.empty) {
			def stat = [success:false, message:"Missing harvesterPackage file attachment."]
			respond stat
			return
		}
		if (!params.id) {
			def stat = [success:false, message:"Missing harvester id."]
			respond stat
			return
		}
		if (params.id in harvesterManager.list()) {
			def stat = [success:false, message:"Harvester id already exists:${params.id}"]
			respond stat
			return
		}
		def packageFile = new File(grailsApplication.config.harvest.base + harvesterPackage.getOriginalFilename())		
		harvesterPackage.transferTo(packageFile)
		respond harvesterManager.add(params.id, params.configPath, packageFile.getAbsolutePath())		
	}
	
	/**
	 * URL: <app-url>/harvester/isStarted/<harvester id>
	 * 
	 * @return JSON: {"started":<boolean>}
	 */
	def isStarted() {
		def stat = [started:harvesterManager.isStarted(params.id)]
		respond stat
	}
}
