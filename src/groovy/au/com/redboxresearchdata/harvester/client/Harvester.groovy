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

import java.util.jar.JarFile
import java.util.zip.ZipEntry
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.springframework.context.ApplicationContext
import org.springframework.context.support.FileSystemXmlApplicationContext
import org.springframework.integration.Message
import org.springframework.integration.MessageChannel
import org.springframework.integration.endpoint.AbstractEndpoint
import org.springframework.integration.message.GenericMessage
import org.springframework.integration.support.MessageBuilder
import org.springframework.core.io.ClassPathResource

/** Represents an instance of a Harvester
 * 
 * 
 * @author Shilo Banihit
 *
 */
class Harvester {
	def config
	def appContext
		
			
	/**
	 * Starts the harvester application context
	 * 
	 * @return
	 */
	def start() {
		if (appContext) {
			log.debug("Harvester has already started, ignoring start request:" + config.web.name)
			return
		}
		config.web.classPathEntries?.each {
			String entryTargetPath = config.web.base + it
			log.debug("Adding to classpath:" + entryTargetPath)
			Harvester.class.classLoader.addClasspath(entryTargetPath)
		}

		ApplicationContext parentContext = (ApplicationContext)config.runtime.parentContext
		String[] locs = ["file:"+config.web.siPath]
		appContext = new FileSystemXmlApplicationContext(locs, true, parentContext)
		appContext.refresh()
		appContext.registerShutdownHook()
		appContext.start()
		log.debug("Harvester started: "+config.web.name)
	}
	
	/**
	 * Stops this harvester and closes the application context
	 * 
	 * @return
	 */
	def stop() {
		if (!appContext) {
			log.debug("Harvester has stopped, ignoring stop request:" + config.web.name)
			return
		}
		log.debug("Harvester stopping:" + config.web.name + ", using inbound adapter:" + config.web.inboundAdapter.toString())		
		AbstractEndpoint inboundEndpoint = appContext.getBean(config.web.inboundAdapter.toString(), AbstractEndpoint.class)
		inboundEndpoint.stop()
		appContext.stop()
		appContext = null
		log.debug("Harvester stopped:" + config.web.name)
	}
}
