<%@ page import="javax.xml.namespace.QName" %>
<%@ page import="javax.xml.soap.*" %>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%
    SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
    SOAPConnection soapConnection = soapConnectionFactory.createConnection();

    // Send SOAP Message to SOAP Server
    String url = "http://SOAP_SERVER_IP/SOMESERVVICE/CheckTokenAccount.asmx?WSDL",
            serverURI = "http://tempuri.org/";

    MessageFactory messageFactory = MessageFactory.newInstance();
    SOAPMessage soapMessage = messageFactory.createMessage();
    SOAPPart soapPart = soapMessage.getSOAPPart();
    SOAPEnvelope envelope = soapPart.getEnvelope();

    // SOAP Body
    SOAPBody soapBody = envelope.getBody();
    QName bodyName = new QName(serverURI, "GetAccount");
    SOAPBodyElement bodyElement = soapBody.addBodyElement(bodyName);
    QName name = new QName("TokenID");
    SOAPElement symbol = bodyElement.addChildElement(name);
    symbol.addTextNode("xxxxx");

    MimeHeaders headers = soapMessage.getMimeHeaders();
    headers.addHeader("SOAPAction", serverURI + "GetAccount");

    // Process the SOAP Response
    SOAPMessage soapResponse = soapConnection.call(soapMessage, url);
    SOAPBody body = soapResponse.getSOAPBody();
    String resText = body.getTextContent();
    soapConnection.close();


%>