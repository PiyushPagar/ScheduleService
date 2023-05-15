package com.revnomix.revseed.Service;


import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class EmailServiceImpl {

    @Autowired
    private JavaMailSender emailSender;
    
    public void sendSimpleMessage(String to,String from, String subject, String text) throws Exception{
        SimpleMailMessage message = new SimpleMailMessage(); 
        message.setFrom(from);
        message.setTo(to); 
        message.setSubject(subject); 
        message.setText(text);
        emailSender.send(message);	        
    }

    public void sendMessageWithAttachment(String to,String from, String subject, String text, File fileObj) throws Exception{
    	MimeMessage message = emailSender.createMimeMessage();       
        MimeMessageHelper helper = new MimeMessageHelper(message, true);        
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text,true);            
        FileSystemResource file = new FileSystemResource(fileObj);
        helper.addAttachment(fileObj.getName(), file);
        emailSender.send(message);
    }
    
    public void sendHTMLMessageWithoutAttachment(String to,String from, String subject, String text) throws Exception{
    	MimeMessage message = emailSender.createMimeMessage();       
        MimeMessageHelper helper = new MimeMessageHelper(message, true);        
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text,true);            
        emailSender.send(message);
    }
    
    public String replaceNameTags (String html, HashMap<String,String> tags) {
		
    	for (Map.Entry<String, String> set : tags.entrySet()) {
    		String replaceString= html.replaceAll(set.getKey(), set.getValue());
    		html = replaceString;
		}
    	return html;
    }

    
    
}
