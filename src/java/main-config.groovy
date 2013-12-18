/**
 * 
 *  Main configuration for the web layer.
 * 
 *  
 */
environments {
	development {		
		harvest {
			// reference to itself
			mainRuntimeConfig = "${userHome}/.grails/${appName}/main-config.groovy"
			// base directory where all harvester-related artifacts are expanded, must end with "/" 
			base = "${userHome}/.grails/${appName}/harvest/"			
			// the default list of clients available to the web wrapper						
			clients = [:]
		}
	}
	production {
		harvest {
			mainRuntimeConfig = "file:${userHome}/.grails/${appName}/main-config.groovy"
			base = "file:${userHome}/.grails/${appName}/harvest/"
			clients = [:]
		}
	}
}