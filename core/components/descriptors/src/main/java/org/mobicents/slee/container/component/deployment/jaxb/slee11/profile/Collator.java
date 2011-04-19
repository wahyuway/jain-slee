/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.1-b02-fcs 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2010.03.17 at 04:43:58 PM WET 
//


package org.mobicents.slee.container.component.deployment.jaxb.slee11.profile;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;


/**
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "description",
    "collatorAlias",
    "localeLanguage",
    "localeCountry",
    "localeVariant"
})
@XmlRootElement(name = "collator")
public class Collator {

    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String strength;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    protected String decomposition;
    @XmlAttribute
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    protected String id;
    protected Description description;
    @XmlElement(name = "collator-alias", required = true)
    protected CollatorAlias collatorAlias;
    @XmlElement(name = "locale-language", required = true)
    protected LocaleLanguage localeLanguage;
    @XmlElement(name = "locale-country")
    protected LocaleCountry localeCountry;
    @XmlElement(name = "locale-variant")
    protected LocaleVariant localeVariant;

    /**
     * Gets the value of the strength property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getStrength() {
        return strength;
    }

    /**
     * Sets the value of the strength property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setStrength(String value) {
        this.strength = value;
    }

    /**
     * Gets the value of the decomposition property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDecomposition() {
        return decomposition;
    }

    /**
     * Sets the value of the decomposition property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDecomposition(String value) {
        this.decomposition = value;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

    /**
     * Gets the value of the description property.
     * 
     * @return
     *     possible object is
     *     {@link Description }
     *     
     */
    public Description getDescription() {
        return description;
    }

    /**
     * Sets the value of the description property.
     * 
     * @param value
     *     allowed object is
     *     {@link Description }
     *     
     */
    public void setDescription(Description value) {
        this.description = value;
    }

    /**
     * Gets the value of the collatorAlias property.
     * 
     * @return
     *     possible object is
     *     {@link CollatorAlias }
     *     
     */
    public CollatorAlias getCollatorAlias() {
        return collatorAlias;
    }

    /**
     * Sets the value of the collatorAlias property.
     * 
     * @param value
     *     allowed object is
     *     {@link CollatorAlias }
     *     
     */
    public void setCollatorAlias(CollatorAlias value) {
        this.collatorAlias = value;
    }

    /**
     * Gets the value of the localeLanguage property.
     * 
     * @return
     *     possible object is
     *     {@link LocaleLanguage }
     *     
     */
    public LocaleLanguage getLocaleLanguage() {
        return localeLanguage;
    }

    /**
     * Sets the value of the localeLanguage property.
     * 
     * @param value
     *     allowed object is
     *     {@link LocaleLanguage }
     *     
     */
    public void setLocaleLanguage(LocaleLanguage value) {
        this.localeLanguage = value;
    }

    /**
     * Gets the value of the localeCountry property.
     * 
     * @return
     *     possible object is
     *     {@link LocaleCountry }
     *     
     */
    public LocaleCountry getLocaleCountry() {
        return localeCountry;
    }

    /**
     * Sets the value of the localeCountry property.
     * 
     * @param value
     *     allowed object is
     *     {@link LocaleCountry }
     *     
     */
    public void setLocaleCountry(LocaleCountry value) {
        this.localeCountry = value;
    }

    /**
     * Gets the value of the localeVariant property.
     * 
     * @return
     *     possible object is
     *     {@link LocaleVariant }
     *     
     */
    public LocaleVariant getLocaleVariant() {
        return localeVariant;
    }

    /**
     * Sets the value of the localeVariant property.
     * 
     * @param value
     *     allowed object is
     *     {@link LocaleVariant }
     *     
     */
    public void setLocaleVariant(LocaleVariant value) {
        this.localeVariant = value;
    }

}
