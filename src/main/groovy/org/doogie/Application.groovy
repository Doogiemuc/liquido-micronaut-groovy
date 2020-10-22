package org.doogie

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import io.micronaut.runtime.Micronaut

@CompileStatic
@Slf4j
class Application {
    static void main(String[] args) {
        Micronaut.run(Application, args)
    }
}
