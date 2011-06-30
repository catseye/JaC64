package com.dreamfabric.jac64;
/**
 * Describe class VICConstants here.
 *
 *
 * Created: Sun Jun 18 21:47:06 2006
 *
 * @author <a href="mailto:Joakim@BOTBOX"></a>
 * @version 1.0
 */
public class VICConstants {

  // First cycles - note that cycle 12 means the 12th cycle into
  // raster (e.g. cyc 11, if first cycle is 0)
  public static final byte VS_INIT = 0;
  public static final byte VS_SPRITE3 = 1;
  public static final byte VS_SPRITE4 = 3;
  public static final byte VS_SPRITE5 = 5;
  public static final byte VS_SPRITE6 = 7;
  public static final byte VS_SPRITE7 = 9;
  public static final byte VS_NODISPLAY = 10;
  public static final byte VS_FETCHBADC12 = 12;
  public static final byte VS_VCRC = 14;
  public static final byte VS_SPRITE_DMAOFF = 15;
  public static final byte VS_40CHARSC17 = 17;
  public static final byte VS_DRAWC18_54 = 18;
  public static final byte VS_38CHARSENDC56 = 56;
  public static final byte VS_40CHARSENDC57 = 57;
  public static final byte VS_SPRITE0_RC = 58;
  public static final byte VS_SPRITE1 = 60;
  public static final byte VS_SPRITE2 = 62;
  public static final byte VS_FINISH = 63;

  public static final int SCAN_RATE = 63;

  // Sprite BA untils...
  public static final int BA_SP0 = 59;
  public static final int BA_SP1 = 61;
  // Next line...
  public static final int BA_SP2 = SCAN_RATE + 0;
  public static final int BA_SP3 = SCAN_RATE + 2;
  public static final int BA_SP4 = SCAN_RATE + 4;
  // Current line...
  public static final int BA_SP5 = 6; // On at 1, off at >6 (e.g. 7)
  public static final int BA_SP6 = 8;
  public static final int BA_SP7 = 10;
  public static final int BA_BADLINE = 54;


  public static final int[][] COLOR_SETS = {
    {
      0xff000000, // 0 Black
      0xffffffff, // 1 White
      0xffe04040, // 2 Red
      0xff60ffff, // 3 Cyan
      0xffe060e0, // 4 Purple
      0xff40e040, // 5 Green
      0xff4040e0, // 6 Blue
      0xffffff40, // 7 Yellow
      0xffe0a040, // 8 Orange
      0xff9c7448, // 9 Brown
      0xffffa0a0, // 10 Lt.Red
      0xff545454, // 11 Dk.Gray
      0xff888888, // 12 Gray
      0xffa0ffa0, // 13 Lt.Green
      0xffa0a0ff, // 14 Lt.Blue
      0xffc0c0c0 // 15 Lt.Gray
    },
    {
      0xff000000
      ,0xffFFFFFF
      ,0xff68372B
      ,0xff70A4B2
      ,0xff6F3D86
      ,0xff588D43
      ,0xff352879
      ,0xffB8C76F
      ,0xff6F4F25
      ,0xff433900
      ,0xff9A6759
      ,0xff444444
      ,0xff6C6C6C
      ,0xff9AD284
      ,0xff6C5EB5
      ,0xff959595
    },
    {
      0xff000000, // 0 Black
      0xffFFFFFF, // 1 White
      0xff744335, // 2 Red
      0xff7CACBA, // 3 Cyan
      0xff7B4890, // 4 Purple
      0xff64974F, // 5 Green
      0xff403285, // 6 Blue
      0xffBFCD7A, // 7 Yellow
      0xff7B5B2F, // 8 Orange
      0xff4f4500, // 9 Brown
      0xffa37265, // 10 Lt.Red
      0xff505050, // 11 Dk.Gra
      0xff787878, // 12 Gray
      0xffa4d78e, // 13 Lt.Gre
      0xff786abd, // 14 Lt.Blu
      0xff9f9f9f // 15 Lt.Gray
    },
    { // Ripped from WinVICE (on XP/PC)
      0xff000000, // 0 Black
      0xffFFFFFF, // 1 White
      0xff894036, // 2 Red
      0xff7abfc7, // 3 Cyan
      0xff8a46ae, // 4 Purple
      0xff68a941, // 5 Green
      0xff3e31a2, // 6 Blue
      0xffd0dc71, // 7 Yellow
      0xff905f25, // 8 Orange
      0xff5c4700, // 9 Brown
      0xffbb776d, // 10 Lt.Red
      0xff555555, // 11 Dk.Gra
      0xff808080, // 12 Gray
      0xffaeea88, // 13 Lt.Gre
      0xff7c70da, // 14 Lt.Blu
      0xffababab  // 15 Lt.Gray
    }
  };
}
