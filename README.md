# Search Engine

The final project of the Professional Java Developer course: 'Search Engine'

## Description

This project is a search engine developed using Spring Boot. It is designed to index web pages and perform searches through a web interface. Users can execute searches using keywords and receive results that include the site name, page title, and snippet.

The application supports the following features:
- Indexing web pages upon request from the web interface
- Utilizing lemmas and indexes to enhance search efficiency
- Storing indexed data in a MySQL database
- Viewing indexing statistics
- Starting, stopping, and reindexing sites via the web interface
- Indexing individual pages

## Technology Stack

- Programming Language: Java
- Framework: Spring Boot
- Database: MySQL
- Data Access: JPARepository
- Web Scraping: JSOUP
- Morphology Library Lucene
- Java Stream API
- Fork/Join Framework
- Multithreading using Thread
- Liquibase
- Lombok

## Local Setup Instructions

1. Clone the repository to your computer using the following command in Git Bash:
	git clone https://github.com/NikitaLozovik/search-engine.git
2. You need to specify a token to access the Maven repository with Lucene Morphology dependencies. To specify the token, find or create the settings.xml file:
	- In Windows, it is located in the directory C:/Users/<Your_User_Name>/.m2
	- In Linux, it is located in the directory /home/<Your_User_Name>/.m2
	- In macOS, it is located at /Users/<Your_User_Name>/.m2
If the settings.xml file does not exist, create it and insert the following code:
``` xml
	<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              https://maven.apache.org/xsd/settings-1.0.0.xsd">
 	   <servers>
    	      <server>
      	         <id>skillbox-gitlab</id>
      		 <configuration>
        	    <httpHeaders>
        	       <property>
            	          <name>Private-Token</name>
            		  <value>glpat-Viu1C6oUSddYB3JdKviW</value>
          	       </property>
                    </httpHeaders>
      		 </configuration>
    	      </server>
  	   </servers>
	</settings>
```
If the file already exists but does not contain the <servers> block, add only this block. If this block exists in the file, add the <server> block from the code snippet above inside it.In the <value> block, you'll find a unique access token. If you encounter a "401 Authorization Error" when trying to fetch dependencies, take the latest access token from the document at this link: https://docs.google.com/document/d/1rb0ysFBLQltgLTvmh-ebaZfJSI7VwlFlEYT9V5_aPjc
3. Load all Maven dependencies.
4. In the /src/main/resources/application.yaml file, configure the following:
For the database, specify the URL, username, and password (all tables will be automatically created using Liquibase).
For indexing, specify userAgent, referrer, timeOut (waiting time for server response), delay (delay between requests to prevent blocking), maxLemmaOccurrencePercentage (coefficient of lemma repetition at which it is excluded from search).
5. Run the Application.main() method.
6. In your browser, navigate to "https://localhost:8080/".

## How to Use

- Dashboard Tab: Contains statistics for all sites
- Management Tab: Provides an interface for managing indexing
- Search Tab: Allows searching through already indexed pages
