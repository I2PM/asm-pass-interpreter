package de.athalis.pass.processmodel.tudarmstadt

object Examples {
  private val defaultIPSize = 100

  private val applicant = FullySpecifiedSubject("Applicant", InternalBehavior(Macro("Main",
    "START",
    Set(
      Action(
        "START",
        State(Some("Select Supervisor"), function = Some(SelectAgents("supervisor", "Supervisor", 1, 1))),
        Set(
          Transition("Prepare")
        )
      ),
      Action(
        "Prepare",
        State(Some("Prepare Travel Application")),
        Set(
          Transition("Send Application", Some(DoTransitionCondition("Done")))
        )
      ),
      Action(
        "Send Application",
        State(function = Some(AutoSend)),
        Set(
          Transition("Receive Response", Some(
            MessageExchangeCondition(
              "Travel Application",
              "Supervisor",
              subjectVariable = Some("supervisor")
            ))
          )
        )
      ),
      Action(
        "Receive Response",
        State(function = Some(AutoReceive)),
        Set(
          Transition("Decide", Some(
            MessageExchangeCondition(
              "Permission denied",
              "Supervisor"
            ))
          ),
          Transition("Book Hotel", Some(
            MessageExchangeCondition(
              "Permission granted",
              "Supervisor"
            ))
          )
        )
      ),
      Action(
        "Decide",
        State(Some("Decide whether filing again")),
        Set(
          Transition("Prepare", Some(DoTransitionCondition("Redo Travel Application"))),
          Transition("SendNoFurther", Some(DoTransitionCondition("Denial accepted")))
        )
      ),
      Action(
        "SendNoFurther",
        State(function = Some(AutoSend)),
        Set(
          Transition("TERMINATE", Some(
            MessageExchangeCondition(
              "No further Travel Application",
              "Supervisor",
              subjectVariable = Some("supervisor")
            ))
          )
        )
      ),
      Action(
        "Book Hotel",
        State(function = Some(ManualSend)),
        Set(
          Transition("Receive Hotel Response", Some(
            MessageExchangeCondition(
              "Hotel Booking Request",
              "HotelBookingInterface"
            ))
          )
        )
      ),
      Action(
        "Receive Hotel Response",
        State(function = Some(ManualReceive)),
        Set(
          Transition("Make travel", Some(
            MessageExchangeCondition(
              "Hotel Booking Response",
              "HotelBookingInterface"
            ))
          )
        )
      ),
      Action(
        "Make travel",
        State(),
        Set(
          Transition("TERMINATE", None)
        )
      ),
      Action(
        "TERMINATE",
        State(function = Some(Terminate())),
        Set.empty
      )
    )
  )), maximumInstanceRestriction = None, attributes = Set(SubjectIsStartSubject, SubjectHasInputPoolSize(defaultIPSize)))


  private val supervisor = FullySpecifiedSubject("Supervisor", InternalBehavior(Macro("Main",
    "START",
    Set(
      Action(
        "START",
        State(function = Some(AutoReceive)),
        Set(
          Transition("Check", Some(
            MessageExchangeCondition(
              "Travel Application",
              "Applicant",
              storeVariable = Some("application")
            ))
          )
        )
      ),
      Action(
        "Check",
        State(Some("Check Travel Application")),
        Set(
          Transition("SendGranted", Some(DoTransitionCondition("Grant Permission"))),
          Transition("SendDenied", Some(DoTransitionCondition("Deny Permission")))
        )
      ),

      Action(
        "SendGranted",
        State(function = Some(AutoSend)),
        Set(
          Transition("SendAdministration", Some(
            MessageExchangeCondition(
              "Permission granted",
              "Applicant",
              subjectVariable = Some("application")
            ))
          )
        )
      ),
      Action(
        "SendAdministration",
        State(function = Some(AutoSend)),
        Set(
          Transition("TERMINATE", Some(
            MessageExchangeCondition(
              "Approved Travel Application",
              "Administration",
              contentVariable = Some("application")
            ))
          )
        )
      ),

      Action(
        "SendDenied",
        State(function = Some(AutoSend)),
        Set(
          Transition("ReceiveFurther", Some(
            MessageExchangeCondition(
              "Permission denied",
              "Applicant",
              subjectVariable = Some("application")
            ))
          )
        )
      ),
      Action(
        "ReceiveFurther",
        State(function = Some(AutoReceive)),
        Set(
          Transition("TERMINATE", Some(
            MessageExchangeCondition(
              "No further Travel Application",
              "Applicant"
            ))
          ),
          Transition("Check", Some(
            MessageExchangeCondition(
              "Travel Application",
              "Applicant",
              storeVariable = Some("application")
            ))
          )
        ),
      ),
      Action(
        "TERMINATE",
        State(function = Some(Terminate())),
        Set.empty
      )
    )
  )), maximumInstanceRestriction = Some(1), attributes = Set(SubjectHasInputPoolSize(defaultIPSize)))


  private val administration = FullySpecifiedSubject("Administration", InternalBehavior(Macro("Main",
    "START",
    Set(
      Action(
        "START",
        State(function = Some(AutoReceive)),
        Set(
          Transition("Archive", Some(
            MessageExchangeCondition(
              "Approved Travel Application",
              "Supervisor"
            ))
          )
        )
      ),
      Action(
        "Archive",
        State(Some("Archive Travel Application")),
        Set(
          Transition("TERMINATE", Some(DoTransitionCondition("Travel Application filed")))
        )
      ),
      Action(
        "TERMINATE",
        State(function = Some(Terminate())),
        Set.empty
      )
    )
  )), attributes = Set(SubjectHasInputPoolSize(defaultIPSize)))

  private val hotelBookingInterface = DefinedInterfaceSubject("HotelBookingInterface", "HotelBookingProcess", "HotelBookingDesk")

  private val allMessageExchanges: Set[MessageExchanges] = Set(
    MessageExchanges(applicant.identifier, supervisor.identifier, Set("Travel Application", "No further Travel Application")),
    MessageExchanges(supervisor.identifier, applicant.identifier, Set("Permission denied", "Permission granted")),
    MessageExchanges(supervisor.identifier, administration.identifier, Set("Approved Travel Application")),

    MessageExchanges(applicant.identifier, hotelBookingInterface.identifier, Set("Hotel Booking Request")),
    MessageExchanges(hotelBookingInterface.identifier, applicant.identifier, Set("Hotel Booking Response"))
  )

  val travelRequestProcess = Process(
    "Travel Request",
    Set(applicant, supervisor, administration, hotelBookingInterface),
    Some(allMessageExchanges)
  )


  private val hotelBookingClient = UndefinedInterfaceSubject("Client")

  private val hotelBookingDesk = FullySpecifiedSubject("HotelBookingDesk", InternalBehavior(Macro("MAIN",
    "START",
    Set(
      Action("START",
        State(function = Some(AutoReceive)),
        Set(
          Transition("Send Response", Some(
            MessageExchangeCondition(
              "Hotel Booking Request",
              "Client",
              storeVariable = Some("vRequest"),
            ))
          )
        )
      ),
      Action("Send Response",
        State(function = Some(AutoSend)),
        Set(
          Transition("TERMINATE", Some(
            MessageExchangeCondition(
              "Hotel Booking Response",
              "Client",
              subjectVariable = Some("vRequest"),
            ))
          )
        )
      ),
      Action(
        "TERMINATE",
        State(function = Some(Terminate())),
        Set.empty
      )
    )
  )), attributes = Set(SubjectHasInputPoolSize(defaultIPSize)))


  val hotelBookingProcess = Process(
    "HotelBookingProcess",
    Set(hotelBookingClient, hotelBookingDesk),
    Some(Set(
      MessageExchanges(hotelBookingClient.identifier, hotelBookingDesk.identifier, Set("Hotel Booking Request")),
      MessageExchanges(hotelBookingDesk.identifier, hotelBookingClient.identifier, Set("Hotel Booking Response"))
    ))
  )
}
