package com.github.juglee0527.apsengine;

import com.github.juglee0527.apsengine.factory.FactoryRepository;
import com.github.juglee0527.apsengine.factory.line.ProductionLineRepository;
import com.github.juglee0527.apsengine.machine.MachineRepository;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration"
})
class ApsEngineApplicationTests {

    @MockitoBean
    private FactoryRepository factoryRepository;

    @MockitoBean
    private ProductionLineRepository productionLineRepository;

    @MockitoBean
    private MachineRepository machineRepository;

    @Test
    void contextLoads() {
    }
}
