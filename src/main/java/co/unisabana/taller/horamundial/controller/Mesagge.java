package co.unisabana.taller.horamundial.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/")
public class Mesagge {

    @GetMapping("/message")
    public ResponseEntity<String> message() {
        return ResponseEntity.ok("hola a todos");
    }
 
}