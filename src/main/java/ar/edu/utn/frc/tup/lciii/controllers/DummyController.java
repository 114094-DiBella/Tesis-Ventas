package ar.edu.utn.frc.tup.lciii.controllers;

import ar.edu.utn.frc.tup.lciii.dtos.DummyDto;
import ar.edu.utn.frc.tup.lciii.models.DummyModel;
import ar.edu.utn.frc.tup.lciii.services.DummyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dummy")

public class DummyController {

    @Autowired
    private DummyService dummyService;

    @GetMapping("")
    public ResponseEntity<DummyDto> getDummyList(){
        List<DummyModel> dummyModels = dummyService.getDummyList();
        return null;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DummyDto> getDummyById(@PathVariable Long dummyId){
        DummyModel dummyModel = dummyService.getDummy(dummyId);
        return null;
    }
    @PostMapping("")
    public ResponseEntity<DummyDto> createDummy(DummyDto dummyDto){
        DummyModel dummyModel = dummyService.createDummy(null);
        return null;
    }
    @PutMapping("")
    public ResponseEntity<DummyDto> updateDummy(DummyDto dummyDto){
        DummyModel dummyModel = dummyService.updateDummy(null);
        return null;
    }
    @DeleteMapping("")
    public ResponseEntity<DummyDto> deleteDummy(DummyDto dummyDto){
        dummyService.delete(null);
        return null;
    }
}
