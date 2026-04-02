package io.github.eendroroy.fuelquota.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SpaController {

    // Silently handle Chrome DevTools auto-probe to avoid NoResourceFoundException stack traces
    @GetMapping("/.well-known/appspecific/com.chrome.devtools.json")
    @ResponseBody
    public ResponseEntity<String> chromeDevTools() {
        return ResponseEntity.ok("{}");
    }

    /**
     * SPA fallback: forward all React Router client-side routes to index.html.
     *
     * Every captured segment uses [^\\..]* (no dot allowed), so paths that contain
     * a file extension — /assets/index.js, /favicon.ico, /vite.svg, etc. — never
     * match and fall through to Spring's static-resource handler instead.
     *
     * Patterns cover the deepest routes declared in App.tsx (2 levels: /admin/dashboard).
     * Add another pattern if deeper routes are introduced.
     */
    @RequestMapping(value = {
        "/",
        "/{l1:[^\\.]*}",
        "/{l1:[^\\.]*}/{l2:[^\\.]*}",
        "/{l1:[^\\.]*}/{l2:[^\\.]*}/{l3:[^\\.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
