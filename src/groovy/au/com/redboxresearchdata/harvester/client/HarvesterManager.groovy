/*******************************************************************************
 *Copyright (C) 2013 Queensland Cyber Infrastructure Foundation (http://www.qcif.edu.au/)
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
package au.com.redboxresearchdata.harvester.client

import au.com.redboxresearchdata.util.config.Config

/**
 * Manages the harvester clients.
 * 
 * @author Shilo Banihit
 *
 */
class HarvesterManager {
	def grailsApplication
	def config
	def parentContext
	protected harvesters
	
	def autoStart() {
		if (!harvesters) {
			load()
		}
		harvesters.each {
			if (it.value.config.web?.autoStart)
				start(it.key)
		}		
	}
	
	/** Starts all loaded harvesters
	 * 
	 * @return
	 */
	public synchronized void startAll() {
		if (!harvesters) {
			load()
		}	
		harvesters.each { 
			start(it.key)
		}
	}
	
	/** Stops all harvesters
	 * 
	 * @return
	 */
	public synchronized void stopAll() {
		harvesters?.each {
			it.value.stop()
		}	
	}
	
	public synchronized Object stop(String harvesterId) {
		if (harvesters[harvesterId]) {
			return harvesters[harvesterId].stop()
		} else {
			return [success:false, message:"No such harvester to stop:" + harvesterId]
		}
	}
	
	/** Stops, reloads configuration and starts all harvesters
	 * 
	 * @return
	 */
	public synchronized void reloadAndStartAll() {
		stopAll()
		load()
		startAll()
	}
	
	/** Loads all harvesters specified in the "harvest.clients" config entry 
	 *  
	 */
	public synchronized void load() {
		if (!config) {
			config = grailsApplication.config
			parentContext = grailsApplication.mainContext
		}
		def harvestBase = new File(config.harvest.base)
		if (!harvestBase.exists()) {
			harvestBase.mkdirs()
		}
		harvesters = [:]
		config.harvest.clientConfigObjs = [:]
		def clientConfigs = config.harvest.clients
		clientConfigs.each { 
			 add(it.key, it.value)
		}
	}
	
	public synchronized Object start(String harvesterId) {
		if (harvesters[harvesterId]) {
			def clientConfigObj = config.clientConfigObjs[harvesterId]
			// set the current config object
			config.clientConfigObj = clientConfigObj
			return harvesters[harvesterId].start()
		} else {
			return [success:false, message:"No such harvester to start:" + harvesterId]
		}
	}
	
	/**
	 * List all harvesters
	 * 
	 * @return
	 */
	public synchronized String[] list() {
		return harvesters.keySet().toArray(new String[0])
	}
	
	/**
	 *  Adds a harvester to the runtime configuration. 
	 * 
	 *  If the configPath is blank, "harvester-config.groovy" is used.
	 * 
	 *  If the packagePath is blank, it is assumed that the configuration file is available in the classpath.
	 * 
	 * @return
	 */
	public synchronized Object add(String harvesterId, String configPath="", String packagePath="") {
		if (harvesters[harvesterId]) {
			def msg = "This harvester is already on the system, ignoring:" + harvesterId
			log.error(msg)			
			return [success:false, message:msg]
		}
		if (packagePath != "") {
			log.debug("Expanding client package:" + packagePath)			
			AntBuilder ant = new AntBuilder()
			ant.unzip(src: packagePath, dest:config.harvest.base + harvesterId, overwrite:false)
		}
		if (configPath == null || configPath == "") {
			configPath = "harvester-config.groovy"
		}
		def binding = [config:config, parentContext:parentContext, configPath:configPath, harvesterId:harvesterId, managerBase:config.harvest.base]
		def clientConfigObj = Config.getConfig(config.environment, configPath, config.harvest.base + harvesterId + "/", binding)
		if (!clientConfigObj) {
			def msg = "Failed to load main config file from class path or system path. Please confirm:" + configPath
			log.error(msg)
			return [success:false, message:msg]
		}
		// add the parentContext
		clientConfigObj.runtime.parentContext = parentContext
		clientConfigObj.runtime.jarPath = packagePath
		// make this harvest configuration available from the global config
		config.clientConfigObjs[harvesterId] = clientConfigObj
		harvesters[harvesterId] = new Harvester(config:clientConfigObj)
		config.runtimeConfig.harvest.clients.put(harvesterId, configPath)
		Config.saveConfig(config.runtimeConfig)
		return [success:true, message:"Added new harvester:"+harvesterId]
	}
	
		
	/**
	 * Remove a harvester from the runtime configuration.
	 * 
	 * Note: previously added entries into the classpath remain
	 * 
	 * @return
	 */
	public synchronized Object remove(String harvesterId) {
		if (harvesters[harvesterId]) {
			harvesters[harvesterId].stop()
			String harvesterDir = config.harvest.base + harvesterId
			log.debug("Deleting harvest directory:" + harvesterDir) 			
			new File(harvesterDir).deleteDir()
			new File(config.harvest.base + harvesterId + ".groovy").delete()
			harvesters.remove(harvesterId)		
			config.runtimeConfig.harvest.clients.remove(harvesterId)
			Config.saveConfig(config.runtimeConfig)
			return [success:true, message:"Removed harvester:"+harvesterId]
		} else {
			def msg = "Tried to remove a non-existent harvester:" + harvesterId
			log.debug(msg)
			return [success:false, message:msg]
		}
	}
	
	/**
	 * Create a harvester from a template.
	 * 
	 * @param harvesterId
	 * @param templateName
	 */
	public synchronized Object createFromTemplate(String harvesterId, String templateName) {
		def retval = create harvesterId using templateName
		return retval
	}
	
	def create(harvesterId) {
		if (!harvesterId) {
			def msg = "No harvester name specified."
			log.error msg
			return [success:false, message:msg]
		}		
		if (harvesters && harvesters[harvesterId]) {
			def msg = "Harvester already exists."
			log.error msg
			return [success:false, message:msg]
		}
		[using: {templateName ->
			if (!templateName) {
				def msg = "Please select a template."
				log.error msg				
				return [success:false, message:msg]
			}
			log.info "Creating ${harvesterId} using ${templateName}"
			if (config.harvest.templates.keySet().contains(templateName)) {
				def targetPath = "${config.harvest.base}${harvesterId}.zip"
				def fs = new FileOutputStream(targetPath)
				def out = new BufferedOutputStream(fs)
				out << new URL(config.harvest.templates[templateName].location).openStream()
				out.close()
				fs.close()				
				return add(harvesterId, "", targetPath)
			} else {
				def msg = "Invalid template name."
				log.error msg
				return [success:false, message:msg]
			}			
		}]
	}
	
	/**
	 * Packages all harvester configuration files into a compressed file.
	 * 
	 * @param harvesterId
	 */
	public synchronized Object pack(String harvesterId, String destFileName = "") {
		def targetDir = new File(config.harvest.base + harvesterId)
		if (!targetDir.exists()) {
			def msg = "Cannot package harvester, it does not exist:" + harvesterId
			log.error msg
			return [success:false, message:msg]
		}
		if (destFileName == null || destFileName == "") {
			destFileName = targetDir.getAbsolutePath() + ".zip"
		} else {
			if (destFileName.indexOf("/") >= 0 || destFileName.indexOf("\\") >= 0) {
				def msg = "Cannot package harvester, the specified destination:'${destFileName}' is invalid. Please specify a file name, not a path."
				log.error msg
				return [success:false, message:msg]
			}
			// make sure the destination is within the harvest.base
			destFileName = config.harvest.base+destFileName			
		}		
		new AntBuilder().zip(destFile:destFileName) {
			fileSet(dir: targetDir.getAbsolutePath())
		}
		def msg = destFileName
		log.debug msg
		return [success:true, message:msg]
	}
	
	/**
	 * List available templates.
	 * @return
	 */
	public String[] listTemplates() {
		return config.harvest.templates.keySet().toArray(new String[0])
	}
	
	/**
	 * Return this template's location
	 * 
	 * @param templateName
	 * @return
	 */
	public Object getTemplateLocation(String templateName) {		
		return config.harvest.templates[templateName]?.location ? [success:true, message:config.harvest.templates[templateName].location] : [success:false, message:"No such template/location: ${templateName}"]
	}
	
	/**
	 * Return this template's description
	 * 
	 * @param templateName
	 * @return
	 */
	public Object getTemplateDescription(String templateName) {		
		return config.harvest.templates[templateName]?.description ? [success:true, message:config.harvest.templates[templateName].description] : [success:false, message:"No such template/description: ${templateName}"] 
	}
	/**
	 * Checks if this harvester has started.
	 * 
	 * @param harvesterId
	 * @return
	 */
	public boolean isStarted(String harvesterId) {
		return harvesters[harvesterId] && harvesters[harvesterId].isStarted() 		
	}		
}
