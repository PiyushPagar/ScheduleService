package com.revnomix.revseed;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.client.RestTemplate;

import com.revnomix.revseed.Util.ConstantUtil;
import com.revnomix.revseed.model.ApplicationParameters;
import com.revnomix.revseed.repository.ApplicationParametersRepository;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

@ComponentScan(basePackages = { "com.revnomix.revseed" })
@SpringBootApplication
@Configuration
public class ScheduleServicesApplication {

	@Autowired
	ApplicationParametersRepository appParameterRepository;

	public static void main(String[] args) {
		SpringApplication.run(ScheduleServicesApplication.class, args);

	}

	@Bean
	public JavaMailSender getJavaMailSender() {
		ApplicationParameters usernameProp = appParameterRepository.findOneByCode(ConstantUtil.SMTP_USERNAME);
		String username = "";
		if (usernameProp != null) {
			username = usernameProp.getCodeDesc();
		}
		ApplicationParameters passwordProp = appParameterRepository.findOneByCode(ConstantUtil.SMTP_PASSWORD);
		String password = "";
		if (passwordProp != null) {
			password = passwordProp.getCodeDesc();
		}
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost("smtp.office365.com");
		mailSender.setPort(587);

		mailSender.setUsername(username);// "revseed@revnomix.com"
		mailSender.setPassword(password);// "Revenue@123"

		Properties props = mailSender.getJavaMailProperties();
		props.put("mail.transport.protocol", "smtp");
		props.put("mail.smtp.auth", "true");
		props.put("mail.smtp.starttls.enable", "true");
		props.put("mail.debug", "true");

		return mailSender;
	}

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, false));
    }

    @Bean
    public Jaxb2Marshaller jaxb2Marshaller() {
        Jaxb2Marshaller jaxb2Marshaller = new Jaxb2Marshaller();
        jaxb2Marshaller.setPackagesToScan("com.revnomix");
        Map<String, Object> properties = new HashMap<>();
        NamespacePrefixMapper namespacePrefixMapper = new NamespacePrefixMapper() {
            @Override
            public String getPreferredPrefix(String s, String s1, boolean b) {
                return "";
            }
        };
        properties.put("com.sun.xml.bind.namespacePrefixMapper", namespacePrefixMapper);
        jaxb2Marshaller.setMarshallerProperties(properties);
        return jaxb2Marshaller;
    }
    
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
  

}
