package com.findme.FindMeBack.Controller.GetItemController.Portal.Dto.PortalInfoDto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
public class Body {
    @JacksonXmlProperty(localName = "item")
    private Item item;

    // getters and setters
}