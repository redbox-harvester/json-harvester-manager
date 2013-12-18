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
	public void startAll() {
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
	public void stopAll() {
		harvesters?.each {
			it.value.stop()
		}	
	}
	
	public void stop(String harvestId) {
		harvesters[harvestId].stop()
	}
	
	/** Stops, reloads configuration and starts all harvesters
	 * 
	 * @return
	 */
	public void reloadAndStartAll() {
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
		harvesters = [:]
		config.harvest.clientConfigObjs = [:]
		def clientConfigs = config.harvest.clients
		clientConfigs.each { 
			 add(it.key, it.value)
		}
	}
	
	public void start(String harvestId) {
		def clientConfigObj = config.clientConfigObjs[harvestId]
		// set the current config object
		config.clientConfigObj = clientConfigObj
		harvesters[harvestId].start()
	}
	
	/**
	 * List all harvesters
	 * 
	 * @return
	 */
	public String[] list() {
		def harvestIds = []
		harvesters.keySet().each {
			harvestIds << it
		}
		return harvestIds
	}
	
	/**
	 * Adds a harvester to the runtime configuration 
	 * 
	 * @return
	 */
	public void add(String configPath, String jarPath="") {
		String harvestId = configPath.lastIndexOf('.').with {it != -1 ? configPath[0..<it] : configPath }.toString()
		if (harvesters[harvestId]) {
			log.error("This harvester is already on the system, ignoring:" + harvestId)
			return
		}
		if (jarPath.size() > 0) {
			log.debug("Adding client classpath:" + jarPath)
			HarvesterManager.class.classLoader.addClasspath(jarPath)
			AntBuilder ant = new AntBuilder()
			ant.unzip(src: jarPath, dest:config.harvest.base + harvestId, overwrite:false)
		}
		def binding = [config:config, parentContext:parentContext, configPath:configPath, harvestId:harvestId]
		def clientConfigObj = Config.getConfig(config.environment, configPath, config.harvest.base + harvestId + "/", binding)
		if (!clientConfigObj) {
			log.error("Failed to load main config file from class path or system path. Please confirm:" + configPath)
			return
		}
		// add the parentContext
		clientConfigObj.runtime.parentContext = parentContext
		clientConfigObj.runtime.jarPath = jarPath
		// make this harvest configuration available from the global config
		config.clientConfigObjs[harvestId] = clientConfigObj
		harvesters[harvestId] = new Harvester(config:clientConfigObj)
	}
	
		
	/**
	 * Remove a harvester from the runtime configuration.
	 * 
	 * Note: previously added entries into the classpath remain
	 * 
	 * @return
	 */
	public void remove(String harvestId) {
		if (harvesters[harvestId]) {
			harvesters[harvestId].stop()
			String harvesterDir = config.harvest.base + harvestId
			log.debug("Deleting harvest directory:" + harvesterDir) 			
			new File(harvesterDir).deleteDir()
			new File(config.harvest.base + harvestId + ".groovy").delete()
			harvesters.remove(harvestId)				
		} else {
			log.debug("Tried to remove a non-existent harvester:" + harvestId)
		}
	}
	
}
