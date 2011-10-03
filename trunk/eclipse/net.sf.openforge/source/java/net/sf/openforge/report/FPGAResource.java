/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package net.sf.openforge.report;


/**
 * A collection of total number of FPGA resources by type, including
 * all I/O types on all pins, clock and reset, LUT (subdivided into
 * LUT, SRL16, Single Port LUT RAM, Dual Port LUT RAM, ROM, MUX),
 * Register, Block RAM (subdivided into Dual port, Single Port), 18x18
 * Multiplier, DCM, and Internal Tri-State.
 *
 * @author  cwu
 * @version $Id: FPGAResource.java 149 2006-06-28 17:34:19Z imiller $
 */
public class FPGAResource
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 149 $";

    private Bels bels;
    private FlopLatches flopLatches;
    private Rams rams;
    private Shifters shifters;
    private Tristates tristates;
    private ClockBuffers clockBuffers;
    private IOBuffers ioBuffers;
    private Logicals logicals;
    private Mults mults;
    
    public FPGAResource ()
    {
        this.bels = new Bels();
        this.flopLatches = new FlopLatches();
        this.rams = new Rams();
        this.shifters = new Shifters();
        this.tristates = new Tristates();
        this.clockBuffers = new ClockBuffers();
        this.ioBuffers = new IOBuffers();
        this.logicals = new Logicals();
        this.mults = new Mults();
    }

    public Bels getBels ()
    {
        return this.bels;
    }
    
    public FlopLatches getFlopLatches ()
    {
        return this.flopLatches;
    }
    
    public Rams getRams ()
    {
        return this.rams;
    }
    
    public Shifters getShifters ()
    {
        return this.shifters;
    }
    
    public Tristates getTristates ()
    {
        return this.tristates;
    }
    
    public ClockBuffers getClockBuffers ()
    {
        return this.clockBuffers;
    }
    
    public IOBuffers getIOBuffers ()
    {
        return this.ioBuffers;
    }
    
    public Logicals getLogicals ()
    {
        return this.logicals;
    }
    
    public Mults getMults ()
    {
        return this.mults;
    }
    
    public int getFlipFlops ()
    {
        return this.flopLatches.getFlipFlop();
    }
    
    public int getTotalLUTs ()
    {
        return getLUTs() + getSRL16s() + getSpLutRams() + getDpLutRams() + getRoms();
    }
    
    public int getLUTs ()
    {
        return this.bels.getLUT();
    }

    public int getBlockRams ()
    {
        return getSpBlockRams() + getDpBlockRams();
    }
    
    public int getSRL16s ()
    {
        return this.shifters.getSRL16();
    }
    
    public int getMULT18X18s ()
    {
        return this.mults.getMULT18X18();
    }
    
    public int getBondedIOBs ()
    {
        return this.ioBuffers.getIB() + this.ioBuffers.getOB() + this.ioBuffers.getIOB();
    }

    public int getClock ()
    {
        return this.clockBuffers.getBUFGP();
    }

    public int getIBs ()
    {
        return this.ioBuffers.getIB();
    }

    public int getOBs ()
    {
        return this.ioBuffers.getOB();
    }
    
    public int getIOBs ()
    {
        return this.ioBuffers.getIOB();
    }
    
    public void addLUT (int lut1Count)
    {
        this.bels.addLUT(lut1Count);
    }
    
    public void addFlipFlop (int flopCount)
    {
        this.flopLatches.addFlipFlop(flopCount);
    }

    public void addSRL16 (int srl16Count)
    {
        this.shifters.addSRL16(srl16Count);
    }

    public void addRom (int romCount)
    {
        this.rams.addRom(romCount);
    }
        
    public int getRoms ()
    {
        return this.rams.getRom();
    }

    public void addSpLutRam (int spLutRamCount)
    {
        this.rams.addSpLutRam(spLutRamCount);
    }
        
    public int getSpLutRams ()
    {
        return this.rams.getSpLutRam();
    }

    public void addDpLutRam (int dpLutRamCount)
    {
        this.rams.addDpLutRam(dpLutRamCount);
    }
        
    public int getDpLutRams ()
    {
        return this.rams.getDpLutRam();
    }
    
    public void addSpBlockRam (int spBlockRamCount)
    {
        this.rams.addSpBlockRam(spBlockRamCount);
    }
    
    public int getSpBlockRams ()
    {
        return this.rams.getSpBlockRam();
    }
    
    public void addDpBlockRam (int dpBlockRamCount)
    {
        this.rams.addDpBlockRam(dpBlockRamCount);
    }
        
    public int getDpBlockRams ()
    {
        return this.rams.getDpBlockRam();
    }
        
    public void addClock (int clockCount)
    {
        this.clockBuffers.addBUFGP(clockCount);
    }

    public void addIB (int ibCount)
    {
        this.ioBuffers.addIB(ibCount);
    }

    public void addOB (int obCount)
    {
        this.ioBuffers.addOB(obCount);
    }
    
    public void addIOB (int iobCount)
    {
        this.ioBuffers.addIOB(iobCount);
    }
    
    public void addMULT18X18 (int mult18x18Count)
    {
        this.mults.addMULT18X18(mult18x18Count);
    }
    
    public void addBUFT (int buftCount)
    {
        this.tristates.addBUFT(buftCount);
    }
    
    public void addResourceUsage (FPGAResource resourceUsage)
    {
        this.bels.addResourceUsage(resourceUsage.getBels());
        this.flopLatches.addResourceUsage(resourceUsage.getFlopLatches());
        this.rams.addResourceUsage(resourceUsage.getRams());
        this.shifters.addResourceUsage(resourceUsage.getShifters());
        this.tristates.addResourceUsage(resourceUsage.getTristates());
        this.clockBuffers.addResourceUsage(resourceUsage.getClockBuffers());
        this.logicals.addResourceUsage(resourceUsage.getLogicals());
        this.mults.addResourceUsage(resourceUsage.getMults());
    }
    
    class Bels
    {
        private int lut;
        
        Bels ()
        {
            lut = 0;
        }
        
        public void addLUT (int lutCount)
        {
            lut += lutCount;
        }
        
        public int getLUT ()
        {
            return lut;
        }

        public void addResourceUsage (Bels bels)
        {
            addLUT(bels.getLUT());
        }
    }
    
    class FlopLatches
    {
        private int flipflop;
        
        FlopLatches ()
        {
            int flipflop = 0;
        }
        
        public void addFlipFlop (int flipflopCount)
        {
            flipflop += flipflopCount;
        }
        
        public int getFlipFlop ()
        {
            return flipflop;
        }
        
        public void addResourceUsage (FlopLatches flopLatches)
        {
            addFlipFlop(flopLatches.getFlipFlop());
        }
    }
    
    class Rams
    {
        private int rom;
        private int spLutRam;
        private int dpLutRam;
        private int spBlockRam;
        private int dpBlockRam;
        
        Rams ()
        {
            rom = 0;
            spLutRam = 0;
            dpLutRam = 0;
            spBlockRam = 0;
            dpBlockRam = 0;
        }

        public void addRom (int romCount)
        {
            rom += romCount;
        }
        
        public int getRom ()
        {
            return rom;
        }
        
        public void addSpLutRam (int spLutRamCount)
        {
            spLutRam += spLutRamCount;
        }
        
        public int getSpLutRam ()
        {
            return spLutRam;
        }
        
        public void addDpLutRam (int dpLutRamCount)
        {
            dpLutRam += dpLutRamCount;
        }
        
        public int getDpLutRam ()
        {
            return dpLutRam;
        }

        public void addSpBlockRam (int spBlockRamCount)
        {
            spBlockRam += spBlockRamCount;
        }
        
        public int getSpBlockRam ()
        {
            return spBlockRam;
        }
        
        public void addDpBlockRam (int dpBlockRamCount)
        {
            dpBlockRam += dpBlockRamCount;
        }
        
        public int getDpBlockRam ()
        {
            return dpBlockRam;
        }
        
        public void addResourceUsage (Rams rams)
        {
            addRom(rams.getRom());
            addSpLutRam(rams.getSpLutRam());
            addDpLutRam(rams.getDpLutRam());
            addSpBlockRam(rams.getSpBlockRam());
            addDpBlockRam(rams.getDpBlockRam());
        }
    }
    
    class Shifters
    {
        int srl16;
        
        Shifters ()
        {
            srl16 = 0;
        }
        
        public void addSRL16 (int srl16Count)
        {
            srl16 += srl16Count;
        }
        
        public int getSRL16 ()
        {
            return srl16;
        }
        
        public void addResourceUsage (Shifters shifters)
        {
            addSRL16(shifters.getSRL16());
        }
    }
    
    class Tristates
    {
        private int buft;
        
        Tristates ()
        {
            buft = 0;
        }
        
        public void addBUFT (int buftCount)
        {
            buft += buftCount;
        }
        
        public int getBUFT ()
        {
            return buft;
        }
        
        public void addResourceUsage (Tristates tristates)
        {
            addBUFT(tristates.getBUFT());
        }
    }
    
    class ClockBuffers
    {
        private int bufgp;
        
        ClockBuffers ()
        {
            bufgp = 0;
        }

        public void addBUFGP (int clockCount)
        {
            bufgp += clockCount;
        }
        
        public int getBUFGP ()
        {
            return bufgp;
        }
        
        public void addResourceUsage(ClockBuffers clockBuffers)
        {
            addBUFGP(clockBuffers.getBUFGP());
        }
    }
    
    class IOBuffers
    {
        private int ibuf;
        private int obuf;
        private int iobuf;
        
        IOBuffers ()
        {
            ibuf = 0;
            obuf = 0;
            iobuf = 0;
        }
        
        public void addIB (int ibufCount)
        {
            ibuf += ibufCount;
        }
        
        public int getIB ()
        {
            return ibuf;
        } 
        
        public void addOB (int obufCount)
        {
            obuf += obufCount;
        }
        
        public int getOB ()
        {
            return obuf;
        }

        public void addIOB (int iobufCount)
        {
            iobuf += iobufCount;
        }
        
        public int getIOB ()
        {
            return iobuf;
        }
        
    }
    
    class Logicals
    {
        Logicals ()
        {
        }
        
        public void addResourceUsage(Logicals logicals)
        {
        }
    }
    
    class Mults
    {
        private int mult18x18;
        
        Mults ()
        {
            mult18x18 = 0;
        }
        
        public void addMULT18X18 (int mult18x18Count)
        {
            mult18x18 += mult18x18Count;
        }
        
        public int getMULT18X18 ()
        {
            return mult18x18;
        }
        
        public void addResourceUsage (Mults mults)
        {
            addMULT18X18(mults.getMULT18X18());
        }
    }
    
}

    
