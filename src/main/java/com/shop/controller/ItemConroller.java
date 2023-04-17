package com.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ItemConroller {

    @GetMapping(value = "/admin/item/new")
    public String itemForm() {
        return "/item/itemForm";
    }
}
