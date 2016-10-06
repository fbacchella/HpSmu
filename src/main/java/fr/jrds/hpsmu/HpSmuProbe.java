package fr.jrds.hpsmu;

import javax.xml.xpath.XPathExpressionException;

import org.apache.log4j.Level;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;

import jrds.probe.HttpXml;
import jrds.starter.XmlProvider;

public class HpSmuProbe extends HttpXml {

    @Override
    protected boolean validateXml(XmlProvider xmlstarter, Document d) {
        try {
            String responseTtype = xmlstarter.getNode(d, "/RESPONSE/OBJECT/PROPERTY[@name='response-type-numeric']").getTextContent();
            int responseCode = jrds.Util.parseStringNumber(responseTtype, 2);
            if ( responseCode > 0) {
                String message = xmlstarter.getNode(d, "/RESPONSE/OBJECT/PROPERTY[@name='response']").getTextContent();
                if (message != null && ! message.isEmpty()) {
                    log(Level.ERROR, message);
                } else {
                    log(Level.ERROR, "Unmanaged error");
                }
                return false;
            }
        } catch (DOMException | XPathExpressionException e) {
            log(Level.ERROR, e, "invalid XML operation: %s", e.getMessage());
            return false;
        }
        return true;
    }

}
