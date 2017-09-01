package com.daacs.unit.service

import com.daacs.framework.auth.service.SessionService
import com.daacs.model.event.ErrorEvent
import com.daacs.service.EventService
import com.daacs.service.EventServiceImpl
import com.lambdista.util.Try
import spock.lang.Specification

/**
 * Created by adistasio on 5/5/17.
 */
class EventServiceSpec extends Specification {

    EventService eventService
    SessionService sessionService

    def setup() {
        sessionService = Mock(SessionService)

        eventService = new EventServiceImpl(sessionService: sessionService)
    }

    def "records error event"() {
        given:
        ErrorEvent errorEvent = new ErrorEvent(error: "We failed")

        when:
        Try<Void> result = eventService.recordEvent(errorEvent)

        then:
        1 * sessionService.getUserId() >> "12345"
        result.isSuccess()
    }
}
