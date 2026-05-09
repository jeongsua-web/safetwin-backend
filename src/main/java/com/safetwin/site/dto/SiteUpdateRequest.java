package com.safetwin.site.dto;

import com.safetwin.entity.Site;
import lombok.Getter;

@Getter
public class SiteUpdateRequest {

    private String name;
    private String address;
    private String bizNumber;
    private Site.Status status;
}
