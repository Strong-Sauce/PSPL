package com.postSale.amcProject.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

// SpaController: handles browser-refresh for Angular routes.
//
// PROBLEM: When a user refreshes the browser at /profile, the browser asks
// Spring Boot for GET /profile. Spring Boot has no server-side route for it
// and would return 404. This controller forwards such GET requests to index.html
// so Angular's client-side router can handle them.
//
// WHY NOT addViewControllers?
// addViewControllers creates ParameterizableViewController which handles ALL HTTP
// methods (GET, POST, PUT...). Using a wildcard pattern caused POST /api/auth/signup
// to be intercepted and treated as a static resource lookup, producing the error
// "No static resource api/auth/signup".
//
// This @Controller uses @GetMapping — it ONLY matches GET requests.
// API calls (POST/PUT/DELETE) are always routed to their @RestController.
// /api/** paths are intentionally NOT listed below.
@Controller
public class SpaController {

    // Forward known Angular GET routes to index.html.
    // Angular boots, reads the URL, and renders the right page client-side.
    @GetMapping(value = {
            "/login",
            "/signup",
            "/forgot-password",
            "/reset-password",
            "/profile",
            "/about",
            "/contact",
            "/products",
            "/products/new",
            "/products/{id}"
    })
    public String forwardToAngular() {
        return "forward:/index.html";
    }
}
