/**
 * 
 *  Main custom configuration for the web layer.
 * 
 *  
 */
environments {
	development {
		file {
			runtimePath = userHome + "/."+appName+"-"+environment+"/config/runtime/main-config.groovy"
			customPath = userHome + "/."+appName+"-"+environment+"/config/custom/main-config.groovy"			
		}		
		harvest {
			// base directory where all harvester-related artifacts are expanded, must end with "/" 
			base = userHome + "/."+appName+"-"+environment+"/harvest/"			
			// the default list of clients available, intentionally blank. The runtime version of this property will get populated as clients are added/removed. Do not modify this property on the custom version.  						
			clients = [:]
			templates {
				redboxSampleJdbcHarvester {
					location = "http://dev.redboxresearchdata.com.au/nexus/service/local/artifact/maven/redirect?r=snapshots&g=au.com.redboxresearchdata&a=redbox-dataset-jdbc-harvester-template&v=LATEST&c=bin&e=zip"					
					description = "The simplistic sample Redbox Dataset JDBC client harvester."
				}
			}
		}
	}
	test {
		file {
			runtimePath = userHome + "/."+appName+"-"+environment+"/config/runtime/main-config.groovy"
			customPath = userHome + "/."+appName+"-"+environment+"/config/custom/main-config.groovy"
		}
		harvest {
			// base directory where all harvester-related artifacts are expanded, must end with "/"
			base = userHome + "/."+appName+"-"+environment+"/harvest/"
			// the default list of clients available, intentionally blank. The runtime version of this property will get populated as clients are added/removed. Do not modify this property on the custom version.
			clients = [:]
			templates {
				redboxSampleJdbcHarvester {
					location = "http://dev.redboxresearchdata.com.au/nexus/service/local/artifact/maven/redirect?r=snapshots&g=au.com.redboxresearchdata&a=redbox-dataset-jdbc-harvester-template&v=LATEST&c=bin&e=zip"
					description = "The simplistic sample Redbox Dataset JDBC client harvester."
				}
			}
		}
	}
	production {
		file {
			runtimePath = userHome + "/."+appName+"-"+environment+"/config/runtime/main-config.groovy"
			customPath = userHome + "/."+appName+"-"+environment+"/config/custom/main-config.groovy"			
		}		
		harvest {
			// base directory where all harvester-related artifacts are expanded, must end with "/" 
			base = userHome + "/."+appName+"-"+environment+"/harvest/"			
			// the default list of clients available, intentionally blank. The runtime version of this property will get populated as clients are added/removed. Do not modify this property on the custom version.  						
			clients = [:]
			templates {
				redboxSampleJdbcHarvester {
					location = "http://dev.redboxresearchdata.com.au/nexus/service/local/artifact/maven/redirect?r=snapshots&g=au.com.redboxresearchdata&a=redbox-dataset-jdbc-harvester-template&v=LATEST&c=bin&e=zip"
					description = "The simplistic sample Redbox Dataset JDBC client harvester."
				}
			}
		}
	}
}