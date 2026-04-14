package org.example;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CompareXmlEDIP {
    public boolean compareXML(String xml1, String xml2, String ignoreString) throws ParserConfigurationException, IOException, SAXException, TransformerException, XPathExpressionException {
        Document doc1 = parseXML(xml1);
        Document doc2 = parseXML(xml2);

        // 检查根节点是否相同
        if (!doc1.getDocumentElement().getNodeName().equals(doc2.getDocumentElement().getNodeName())) {
            return false;
        }

        // 检查所有子节点是否相同
        return compareNodes(doc1.getDocumentElement(), doc2.getDocumentElement(), ignoreString);
    }

    private Document parseXML(String xml) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new StringReader(xml)));
    }

    private boolean compareNodes(Node node1, Node node2, String ignoreString) throws TransformerException, XPathExpressionException {
        if (node1.getNodeType() != node2.getNodeType()) {
            return false;
        }

        if (node1.getNodeType() == Node.ELEMENT_NODE) {
//
//            List<String> ignoreList = new ArrayList<>();
//            String[] strings = ignoreString.split(",");
//            for(String a :strings ){
//                ignoreList.add(a);
//            }
//            //豁免比对
//            if(ignoreList.contains(node1.getNodeName())){
//                return true;
//            }

            //豁免比对
            if(ignoreString.contains(node1.getNodeName())){
                return true;
            }


            if (!node1.getNodeName().equals(node2.getNodeName())) {
                return false;
            }

            // 比较属性
            if (!compareAttributes(node1, node2)) {
                return false;
            }

            // 比较子节点
            NodeList children1 = node1.getChildNodes();
            NodeList children2 = node2.getChildNodes();

            if (children1.getLength() != children2.getLength()) {
                return false;
            }

            for (int i = 0; i < children1.getLength(); i++) {
                Node child1 = children1.item(i);
                Node child2 = children2.item(i);

                if (!compareNodes(child1, child2,ignoreString)) {
                    return false;
                }
            }
        } else if (node1.getNodeType() == Node.TEXT_NODE) {
            if (!node1.getNodeValue().trim().equals(node2.getNodeValue().trim())) {
                return false;
            }
        }

        return true;
    }

    private boolean compareAttributes(Node node1, Node node2) {
        if (node1.getAttributes().getLength() != node2.getAttributes().getLength()) {
            return false;
        }

        for (int i = 0; i < node1.getAttributes().getLength(); i++) {
            Node attr1 = node1.getAttributes().item(i);
            Node attr2 = node2.getAttributes().getNamedItem(attr1.getNodeName());

            if (attr2 == null || !attr1.getNodeValue().equals(attr2.getNodeValue())) {
                return false;
            }
        }

        return true;
    }
}
