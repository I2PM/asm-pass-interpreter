package de.athalis.asm.test.semantic

import de.athalis.asm.test.TestAllCasm

class TestIPSize extends TestAllCasm
class TestReservation extends TestAllCasm
class TestChannel extends TestAllCasm
class TestTransitions extends TestAllCasm {
  override def outFilter(in: String): Boolean = in.contains("TestAllCasmOutput")
}
class TestTransitions2Tests extends TestAllCasm
class TestVarman extends TestAllCasm
