package com.revnomix.revseed.Util.service;

import java.io.IOException;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.compress.utils.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.stereotype.Service;

import com.revnomix.revseed.integration.exception.RevseedException;

@Service
public class XMLUtilService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    public String convertFromObjectToXMLStringWithoutXMLRoot(Marshaller marshaller, Object object,Class clazz) {
        JAXBElement<Object> jaxbElement =
                new JAXBElement<Object>( new QName("", clazz.getName()),
                        clazz,
                        object);
        logger.debug("Converting object: " + object + " to XML");
        StringWriter sw = new StringWriter();
        try {
            marshaller.marshal(jaxbElement, new StreamResult(sw));
        } catch (IOException ioe) {
            throw new RevseedException("ERROR : An issue while marshalling data ", ioe);
        } finally {
            IOUtils.closeQuietly(sw);
        }
        return sw.toString();
    }

    public String convertFromObjectToXMLString(Marshaller marshaller, Object object) {
        logger.debug("Converting object: " + object + " to XML");
        StringWriter sw = new StringWriter();
        try {
            marshaller.marshal(object, new StreamResult(sw));
        } catch (IOException ioe) {
            throw new RevseedException("ERROR : An issue while marshalling data ", ioe);
        } finally {
            IOUtils.closeQuietly(sw);
        }
        return sw.toString();
    }

    @SuppressWarnings("unchecked")
    public <T> T convertFromXMLToObject(Marshaller marshaller, String xmlToConvert) {
        logger.debug("Converting XML to Object...");

        Reader reader = null;
        try {
            reader = new StringReader(xmlToConvert);
            Source source = new StreamSource(reader);

            // If the return object is a JAXBElement, just have it return it's value
            // No reason to spread the JAXBElement where it doesn't need to be
            Object retVal = ((Unmarshaller) marshaller).unmarshal(source);
            if (retVal instanceof JAXBElement<?>) {
                return (T) ((JAXBElement<?>) retVal).getValue();
            }
            return (T) retVal;
        } catch (IOException e) {
            throw new RevseedException("ERROR while unmarshalling data from :" + xmlToConvert, e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }
}
