package org.meb.oneringdb.db.util;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XPathUtil implements Serializable {

	private static final long serialVersionUID = -1220593851813834651L;

	transient private Map<String, XPathExpression> exprMap;
	transient private Map<String, Object> resultMap;
	transient private static XPathFactory xpathFactory;
	transient private XPath xpath;
	private boolean cacheable;

	public XPathUtil(boolean cacheable) {
		this.cacheable = cacheable;
	}

	protected synchronized XPathFactory getXPathFactory() {
		if (xpathFactory == null) {
			xpathFactory = XPathFactory.newInstance();
		}
		return xpathFactory;
	}

	protected XPath getXPath() {
		if (xpath == null) {
			xpath = getXPathFactory().newXPath();
		}
		return xpath;
	}

	private Map<String, XPathExpression> getExprMap() {
		if (exprMap == null) {
			exprMap = new HashMap<String, XPathExpression>();
		}
		return exprMap;
	}

	private Map<String, Object> getResultMap() {
		if (resultMap == null) {
			resultMap = new HashMap<String, Object>();
		}
		return resultMap;
	}

	public final Object get(Object item, String exprString, QName returnType)
			throws XPathExpressionException {

		Object result = getResultMap().get(exprString);
		if (cacheable && result != null) {
			return result;
		}

		XPathExpression expr = getExprMap().get(exprString);
		if (expr == null) {
			expr = getXPath().compile(exprString);
			getExprMap().put(exprString, expr);
		}
		result = expr.evaluate(item, returnType);
		if (cacheable) {
			getResultMap().put(exprString, result);
		}
		return result;
	}

	public Node getNode(Object item, String exprString) throws XPathExpressionException {
		return (Node) get(item, exprString, XPathConstants.NODE);
	}
	
	public Node getNode(Object item, String exprString, boolean silent) {
		try {
			return getNode(item, exprString);
		} catch (XPathExpressionException e) {
			if (silent) {
				return null;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	public NodeList getNodeList(Object item, String exprString) throws XPathExpressionException {
		return (NodeList) get(item, exprString, XPathConstants.NODESET);
	}
	
	public NodeList getNodeList(Object item, String exprString, boolean silent) {
		try {
			return getNodeList(item, exprString);
		} catch (XPathExpressionException e) {
			if (silent) {
				return null;
			} else {
				throw new RuntimeException(e);
			}
		}
	}

	public Boolean getBooleanNode(Object item, String exprString, Boolean defa) {
		try {
			Boolean value = defa;
			Node node = getNode(item, exprString);
			if (node != null) {
				value = Boolean.valueOf(node.getTextContent());
			}
			return value;
		} catch (XPathExpressionException e) {
			return defa;
		}
	}

	public Boolean getBooleanNode(Object item, String exprString) {
		return getBooleanNode(item, exprString, null);
	}

	public String getStringNode(Object item, String exprString, String defa) {
		try {
			return (String) get(item, exprString, XPathConstants.STRING);
		} catch (XPathExpressionException e) {
			return defa;
		}
	}

	public String getStringNode(Object item, String exprString) {
		return getStringNode(item, exprString, null);
	}

	public BigDecimal getNumberNode(Object item, String exprString, BigDecimal defa) {
		try {
			BigDecimal value = defa;
			Node node = getNode(item, exprString);
			if (node != null) {
				value = new BigDecimal(node.getTextContent());
			}
			return value;
		} catch (XPathExpressionException e) {
			return defa;
		} catch (NumberFormatException e1) {
			return defa;
		}
	}

	public BigDecimal getNumberNode(Object item, String exprString) {
		return getNumberNode(item, exprString, null);
	}
}
