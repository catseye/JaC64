/**
 * This file is a part of JaC64 - a Java C64 Emulator
 * Main Developer: Joakim Eriksson (Dreamfabric.com)
 * Contact: joakime@sics.se
 * Web: http://www.dreamfabric.com/c64
 * ---------------------------------------------------
 */

package com.dreamfabric.jac64;

import java.util.Hashtable;

/**
 * ExtChip - used for implementing HW Chips connected to the
 * CPU.
 * handles IRQs/NMIs for all the implemented CPU/IO chips
 * and defines some APIs that is called by CPU
 *
 *
 * Created: Tue Aug 02 08:58:12 2005
 *
 * @author Joakim Eriksson
 * @version 1.0
 */
public abstract class ExtChip {

  public static final boolean DEBUG_INTERRUPS = false;

  // C64 specific names - but... basically just numbers
  public static final int VIC_IRQ = 1;
  public static final int CIA_TIMER_IRQ = 2;
  public static final int KEYBOARD_NMI = 1;
  public static final int CIA_TIMER_NMI = 2;

  // One InterruptManager per named CPU.
  private static Hashtable<String, InterruptManager> managers = 
    new Hashtable<String, InterruptManager>();
  MOS6510Core cpu;
  private Observer observer;
  private InterruptManager im;
  
  public void init(MOS6510Core cpu) {
    this.cpu = cpu;
    if (managers.get(cpu.getName()) == null) {
        System.out.println("creating new IM...");
        managers.put(cpu.getName(), new InterruptManager(cpu));
    }
    im = (InterruptManager) managers.get(cpu.getName());
  }

  public void deleteInterruptManagers() {
    managers = new Hashtable<String, InterruptManager>();
  }

  public int getNMIFlags() {
    return im.nmiFlags;
  }

  public int getIRQFlags() {
    return im.irqFlags;
  }

  public boolean setIRQ(int irq) {
    return im.setIRQ(irq);
  }
  
  public void clearIRQ(int irq) {
    im.clearIRQ(irq);
  }
  
  public boolean setNMI(int nmi) {
    return im.setNMI(nmi);
  }
  
  public void clearNMI(int nmi) {
    im.clearNMI(nmi);
  }
  
  public void resetInterrupts() {
    im.reset();
  }

  
  public abstract void reset();
  public abstract void stop();

  public abstract int performRead(int address, long cycles);
  public abstract void performWrite(int address, int data, long cycles);
  public abstract void clock(long cycles);

  public void setObserver(Observer o) {
    observer = o;
  }

  public void update(Object source, Object data) {
    if (observer != null) {
      observer.update(source, data);
    }
  }
  
  private static class InterruptManager {
    int nmiFlags;
    int irqFlags;
    int oldIrqFlags;
    int oldNmiFlags;
    MOS6510Core cpu;
    
    InterruptManager(MOS6510Core cpu) {
      this.cpu = cpu;
    }

    private void reset() {
      nmiFlags = 0;
      irqFlags = 0;
      oldIrqFlags = 0;
      oldNmiFlags = 0;
      cpu.setIRQLow(false);
      cpu.setNMILow(false);
      cpu.log("ExtChip: Resetting IRQ flags!");
    }
    
    public boolean setIRQ(int irq) {
    	boolean val = (irqFlags & irq) == 0;
    	irqFlags |= irq;
    	if (irqFlags != oldIrqFlags) {
    		if (DEBUG_INTERRUPS && irqFlags != 0 && cpu.debug) {
    			cpu.log("ExtChips: Setting IRQ! " + irq + " => " + irqFlags +  " at " + cpu.cycles);
    		}
    		cpu.setIRQLow(irqFlags != 0);
    		oldIrqFlags = irqFlags;
    	}
    	return val;
    }

    public void clearIRQ(int irq) {
    	irqFlags &= ~irq;
    	if (irqFlags != oldIrqFlags) {
    		if (DEBUG_INTERRUPS && oldIrqFlags != 0 && cpu.debug) {
    			System.out.println("Clearing IRQ! " + irq + " => " + irqFlags +  " at " + cpu.cycles);
    		}
    		cpu.setIRQLow(irqFlags != 0);
    		oldIrqFlags = irqFlags;
    	}
    }

    public boolean setNMI(int nmi) {
      boolean val = (nmiFlags & nmi) == 0;
      nmiFlags |= nmi;
      if (nmiFlags != oldNmiFlags) {
        if (DEBUG_INTERRUPS && cpu.debug)
          System.out.println("Setting NMI! " + nmi + " => " + nmiFlags +  " at " + cpu.cycles);
        cpu.setNMILow(nmiFlags != 0);
        oldNmiFlags = nmiFlags;
      }
      return val;
    }


    public void clearNMI(int nmi) {
      nmiFlags &= ~nmi;
      if (nmiFlags != oldNmiFlags) {
        if (DEBUG_INTERRUPS && oldNmiFlags != 0 && cpu.debug) {
          System.out.println("Clearing NMI! " + nmi + " => " + nmiFlags +  " at " + cpu.cycles);
        }
        cpu.setNMILow(nmiFlags != 0);
        oldNmiFlags = nmiFlags;
      }
    }
  }
}