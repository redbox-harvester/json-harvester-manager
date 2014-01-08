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
	
	public synchronized void stop(String harvesterId) {
		harvesters[harvesterId].stop()
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
	protected void load() {
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
	
	public synchronized void start(String harvesterId) {
		def clientConfigObj = config.clientConfigObjs[harvesterId]
		// set the current config object
		config.clientConfigObj = clientConfigObj
		harvesters[harvesterId].start()
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
	public synchronized void add(String harvesterId, String configPath="", String packagePath="") {
		if (harvesters[harvesterId]) {
			log.error("This harvester is already on the system, ignoring:" + harvesterId)
			return
		}
		if (packagePath != "") {
			log.debug("Adding client classpath:" + packagePath)
			HarvesterManager.class.classLoader.addClasspath(packagePath)
			AntBuilder ant = new AntBuilder()
			ant.unzip(src: packagePath, dest:config.harvest.base + harvesterId, overwrite:false)
		}
		if (configPath == "") {
			configPath = "harvester-config.groovy"
		}
		def binding = [config:config, parentContext:parentContext, configPath:configPath, harvesterId:harvesterId]
		def clientConfigObj = Config.getConfig(config.environment, configPath, config.harvest.base + harvesterId + "/", binding)
		if (!clientConfigObj) {
			log.error("Failed to load main config file from class path or system path. Please confirm:" + configPath)
			return
		}
		// add the parentContext
		clientConfigObj.runtime.parentContext = parentContext
		clientConfigObj.runtime.jarPath = packagePath
		// make this harvest configuration available from the global config
		config.clientConfigObjs[harvesterId] = clientConfigObj
		harvesters[harvesterId] = new Harvester(config:clientConfigObj)
		config.runtimeConfig.harvest.clients.put(harvesterId, configPath)
		Config.saveConfig(config.runtimeConfig)
	}
	
		
	/**
	 * Remove a harvester from the runtime configuration.
	 * 
	 * Note: previously added entries into the classpath remain
	 * 
	 * @return
	 */
	public synchronized void remove(String harvesterId) {
		if (harvesters[harvesterId]) {
			harvesters[harvesterId].stop()
			String harvesterDir = config.harvest.base + harvesterId
			log.debug("Deleting harvest directory:" + harvesterDir) 			
			new File(harvesterDir).deleteDir()
			new File(config.harvest.base + harvesterId + ".groovy").delete()
			harvesters.remove(harvesterId)		
			config.runtimeConfig.harvest.clients.remove(harvesterId)
			Config.saveConfig(config.runtimeConfig)
		} else {
			log.debug("Tried to remove a non-existent harvester:" + harvesterId)
		}
	}
	
	/**
	 * Create a harvester from a template.
	 * 
	 * @param harvesterId
	 * @param templateName
	 */
	public synchronized void createFromTemplate(String harvesterId, String templateName) {
		create harvesterId using templateName
	}
	
	def create(harvesterId) {
		if (!harvesterId) {
			def msg = "No harvester name specified."
			log.error msg
			return msg
		}		
		if (harvesters && harvesters[harvesterId]) {
			def msg = "Harvester already exists."
			log.error msg
			return msg
		}
		[using: {templateName ->
			if (!templateName) {
				def msg = "Please select a template."
				log.error msg				
				return msg
			}
			log.info "Creating ${harvesterId} using ${templateName}"
			if (config.harvest.templates.keySet().contains(templateName)) {
				def targetPath = "${config.harvest.base}${harvesterId}.zip"
				def fs = new FileOutputStream(targetPath)
				def out = new BufferedOutputStream(fs)
				out << new URL(config.harvest.templates[templateName].location).openStream()
				out.close()
				fs.close()				
				add(harvesterId, "", targetPath)
			} else {
				def msg = "Invalid template name."
				log.error msg
				return msg
			}
			return true
		}]
	}
	
	/**
	 * Packages all harvester configuration files into a compressed file.
	 * 
	 * @param harvesterId
	 */
	public synchronized void pack(String harvesterId, String destFileName = "") {
		def targetDir = new File(config.harvest.base + harvesterId)
		if (!targetDir.exists()) {
			log.error "Cannot package harvester, it does not exist:" + harvesterId
			return
		}
		if (destFileName == "") {
			destFileName = targetDir.getAbsolutePath() + ".zip"
		}		
		new AntBuilder().zip(destFile:destFileName) {
			fileSet(dir: targetDir.getAbsolutePath())
		}
		log.debug("Harvester '${harvesterId}' packaged to: ${destFileName}")
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
	public String getTemplateLocation(String templateName) {
		if (!config.harvest.templates[templateName]?.location) {
			return "No such template/location: ${templateName}" 
		}
		return config.harvest.templates[templateName].location
	}
	
	/**
	 * Return this template's description
	 * 
	 * @param templateName
	 * @return
	 */
	public String getTemplateDescription(String templateName) {
		if (!config.harvest.templates[templateName]?.description) {
			return "No such template/description: ${templateName}"
		}
		return config.harvest.templates[templateName].description
	}		
}
