/*
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
 */
package au.com.redboxresearchdata.harvester.client

import java.util.jar.JarFile
import java.util.zip.ZipEntry
import org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader
import org.springframework.context.ApplicationContext
import org.springframework.context.support.FileSystemXmlApplicationContext
import org.springframework.context.support.GenericApplicationContext
import org.springframework.integration.Message
import org.springframework.integration.MessageChannel
import org.springframework.integration.endpoint.AbstractEndpoint
import org.springframework.integration.message.GenericMessage
import org.springframework.integration.support.MessageBuilder
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource

/** Represents an instance of a Harvester
 * 
 * 
 * @author <a href="https://github.com/shilob" target="_blank">Shilo Banihit</a>
 * @since 1.0
 *
 */
class Harvester {
	def config
	def appContext
	def siThread	
			
	/**
	 * Starts the harvester application context
	 * 
	 * @return
	 */
	def start() {		
		if (appContext) {
			def msg = "Harvester has already started, ignoring start request:" + config.client.harvesterId
			log.debug(msg)
			return [success:false, message:msg]
		}
		config.client.classPathEntries?.each {
			String entryTargetPath = config.client.base + it					
			addToClasspath(entryTargetPath)					
		}
		def msg = "Harvester started: "+config.client.harvesterId 
		if (config.client.spawnThread) {
			log.debug("Preparing to start SI context: "+config.client.harvesterId)
			ApplicationContext parentContext = (ApplicationContext)config.runtime.parentContext
			appContext = new GenericApplicationContext(parentContext)
			XmlBeanDefinitionReader xmlReader = new XmlBeanDefinitionReader(appContext)
			xmlReader.loadBeanDefinitions(new FileSystemResource(config.client.siPath))
			siThread = Thread.start {
				log.debug("Starting SI context: "+config.client.harvesterId)
				appContext.refresh()
				log.debug("Stopped SI context, thread exiting: "+config.client.harvesterId)
			}
			msg = "Harvester started asynchly, you may want to check its status later: "+config.client.harvesterId
		} else {
			ApplicationContext parentContext = (ApplicationContext)config.runtime.parentContext
			String[] locs = ["file:"+config.client.siPath]
			appContext = new FileSystemXmlApplicationContext(locs, true, parentContext)
			appContext.registerShutdownHook()
		}
		log.debug(msg)
		return [success:true, message:msg]
	}
	
	/**
	 * Stops this harvester and closes the application context
	 * 
	 * @return
	 */
	def stop() {
		if (!appContext) {
			def msg = "Harvester has stopped, ignoring stop request:" + config.client.harvesterId
			log.debug(msg)
			return [success:false, message:msg]
		}
		log.debug("Harvester stopping:" + config.client.harvesterId + ", using mbean exporter:" + config.client.mbeanExporter.toString())
		def mbeanExporter = appContext.getBean(config.client.mbeanExporter.toString())
		def gracefulShutdownTimeout = config.client.orderlyShutdownTimeout ? config.client.orderlyShutdownTimeout : 5000 // defaults to 5 seconds			
		mbeanExporter.stopActiveComponents(false, gracefulShutdownTimeout) 
		log.debug("Mbeanexporter stopActiveComponents returned, stopping appContext...")
		appContext.stop()
		appContext = null
		siThread = null
		def msg = "Harvester stopped:" + config.client.harvesterId
		log.debug(msg)
		return [success:true, message:msg]
	}
	
	def isStarted() {
		return appContext != null 
	}
	
	def addToClasspath(String entryTargetPath) {
		File entryPathFile = new File(entryTargetPath)
		if (!entryPathFile.exists()) {
			log.error("Attempted to add a non-existent file: ${entryTargetPath}")
			return
		}
		log.debug "Attempting to dynamically add stuff to the classpath..."
		def classLoader = FileSystemXmlApplicationContext.class.getClassLoader()		
		classLoader.addURL(entryPathFile.toURI().toURL())						
		log.debug("Added to classpath:" + entryTargetPath)
	}
}
