DAACS REST API
--------------
The backend/API portion of the Diagnostic Assessment & Achievement of College Skills web application 

# Technologies
- Java 1.8.0_91
- Groovy 2.4.7
- Spring Boot 1.3.5
- MongoDB 3.2.7
- LightSide 2.3.1
- Spring Boot 1.3.5
- Spring Security SAML Extension 1.0.2
- Jetty 9.2.16
- Jackson 2.6.6
- Apache Commons Lang3 3.4
- Apache Commons IO 2.5
- Apache Commons File Upload 1.3.2
- Hystrix 1.3.19
- Try 0.3.1
- Guava 19.0
- Orika Mapper 1.4.6
- Springfox Swagger 2.5.0
- OpenCSV 3.8
- Spock Framework 1.0-groovy-2.4
- cglib 3.2.2
- objensis 2.4

# LightSide
Automated grading for writing assessments done using LightSide http://ankara.lti.cs.cmu.edu/side/

# Settings
The following settings can be set by creating a ```daacs.properties``` file and setting the environment variable ```daacsapiProperties``` to the location of the file.

## Authentication
DAACS comes pre-equiped with SAML and direct OAuth2 authentication. Both can be used in tandem.


### SAML Login

#### Setting up the keystore
DAACS uses the Spring Security SAML Extension to manage SAML interactions. Part of this is setting up a keystore containing the certificates required to communicate with the SAML IDP.
Exact instructions on how to do this can be found at: 
```
http://docs.spring.io/spring-security-saml/docs/1.0.0.RELEASE/reference/html/security.html#configuration-key-management
```

The keystore location and other properties can be configured in the ```daacs.properties``` file. 
```
saml.entityId=com:gavant:daacs:test
saml.frontendAuthSuccessUrl=http://frontend/auth/success
saml.frontendAuthFailureUrl=http://frontend/auth/failure
saml.keystorePath=classpath:/saml/samlKeystore.jks
saml.keystoreDefaultKey=daacs_key
saml.keystorePassword=daacs123
```

#### Specifying the IDP SAML metadata
The IDP SAML metadata path can be specified in the ```daacs.properties``` file.
```
saml.idpMetadataPath=/path/to/my/metdata.xml
```

#### Retrieving DAACS's SP SAML metadata
The SAML metadata for DAACS is automatically generated and can be found by hitting the ```/saml/metadata``` endpoint.

#### Exchanging SAML token for OAuth2 token
Once the SAML handshake has happened, the client can exchange the SAML token with an OAuth2 token to ensure the session has been registered by sending POST request to the ```/oauth/token``` endpoint using a grant_type of "saml" and adding including the token value in the "token" field.
```
/oauth/token?token=<SAML TOKEN HERE>&client_id=web&grant_type=saml&scope=read%20write
```

#### Connecting SAML properties to DAACS
We have to tell DAACS which SAML properties correspond to which DAACS user fields, we can specify these in the ```daacs.properties``` file.
```
saml.userFieldConfig.roleAttribute=role
saml.userFieldConfig.uniqueIdAttribute=uuid
saml.userFieldConfig.firstNameAttribute=firstName
saml.userFieldConfig.lastNameAttribute=lastName
saml.userFieldConfig.adminRole=admin
saml.userFieldConfig.studentRole=student
saml.userFieldConfig.advisorRole=advisor
```
Where the values are the field names returned in the SAML handshake.

### LTI Login
The Tool Consumer will need to set the Launch/cartridge URL to: ```<url of DAACS instance>/lti```
and pass in each users username as a custom parameter named ```'username'```

The secret key and other properties can be configured in the ```daacs.properties``` file. 
```
lti.enabled=true
lti.oauth.version=1.0
lti.oauth.signature_method=HMAC-SHA1
lti.oath.timeout_interval=3
lti.oauth.sharedSecret=secret-abc
lti.frontendAuthSuccessUrl=http://localhost:3000/
```

#### Exchanging LTI Oauth token for OAuth2 token
Similar to SAML login once the LTI handshake has happened, the DAACS frontend will automatically exchange the LTI token with an OAuth2 token to ensure the session has been registered by sending POST request to the ```/oauth/token``` endpoint using a grant_type of "lti" and adding including the token value in the "token" field.
```
/oauth/token?token=<LTI TOKEN HERE>&client_id=web&grant_type=lti&scope=read%20write
```

### Direct Login (OAuth2)

#### Adding a user
Users can be added directly to the MongoDB ```Users``` collection. An example document:
```
{
    "_class" : "com.daacs.model.User",
    "username" : "student123",
    "password" : "d7683e52af93b105a44fcef5bd668a77fafd49f9",
    "firstName" : "John",
    "lastName" : "Student",
    "roles" : [ 
        "ROLE_ADMIN"
    ],
    "createdDate" : ISODate("2017-01-04T20:29:55.670Z"),
    "version" : NumberLong(1)
}
```

Passwords are SHA1 (no salt) encrypted.

#### Getting an OAuth2 token
The client can get an OAuth2 token to ensure the session has been registered by sending POST request to the ```/oauth/token``` endpoint using a grant_type of "password" and the appropriate username and password values.
```
/oauth/token?username=student123&password=temp123&client_id=web&grant_type=password&scope=read%20write
```
### Canvas Integration
Instance can be configured so that upon completion of all assessments we will send a course completion message to Canvas.

DAACS requires the following properties to do so 

```
canvas.enabled=true
canvas.baseURL=
canvas.oAuthToken=
canvas.courseId=
canvas.assignmentId=
```

And each student will need to be created with a ```canvasSisId```
If authentication is done through SAML the users ```canvasSisId``` field can be specified with the following propertie 
```
saml.userFieldConfig.canvasSisIdAttribute=
``` 

### E-mail

#### SMTP
```
spring.mail.host=smtp.myserver.com
spring.mail.port=587
spring.mail.username=myuser
spring.mail.password=mypass
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

#### Help message
```
mail.help.toAddress=daacs@mycompany.com
mail.help.fromAddress=daacs@mycompany.com
mail.help.subject=HELP HELP HELP
mail.help.preface=Some text describing what this email is.
```

# Swagger
Documentation for all endpoints can be found with the built-in Swagger documentation by visiting the ```/swagger-ui.html``` page

# MongoDB Pub/Sub
DAACS implements a pub/sub architecture to schedule grading jobs. Information on how this was created with MongoDB can be found [here](http://tugdualgrall.blogspot.fr/2015/01/how-to-create-pubsub-application-with.html).

-----

Diagnostic Assessment and Achievement of College Skills (DAACS)  
Copyright (C) 2017 Excelsior College; The Research Foundation, SUNY at the University at Albany; and Rutgers, The State University of New Jersey.

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation version 3 of the License.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.