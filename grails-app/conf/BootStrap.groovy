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
import org.springframework.core.io.ClassPathResource
import grails.util.Environment
import au.com.redboxresearchdata.util.config.Config

class BootStrap {

	def grailsApplication
	def harvesterManager
	
    def init = { servletContext ->		
		BootStrap.class.classLoader.addClasspath(grailsApplication.config.harvest.base)
		def userHome = grailsApplication.config.userHome
		def appName = grailsApplication.config.appName
		// load the main web runtime configuration
		def binding = [userHome:userHome, appName:appName]
		def env = Environment.current.toString().toLowerCase()
		def baseDir = "${userHome}/.${appName}-${env}/"		
		def runtimeConfig = Config.getConfig(env, "main-config.groovy", baseDir, binding)								
		grailsApplication.config.runtimeConfig = runtimeConfig
		harvesterManager.autoStart()		
    }
    def destroy = {
		log.debug("Harvester web client stopping...")
		harvesterManager.stopAll()
    }
}
