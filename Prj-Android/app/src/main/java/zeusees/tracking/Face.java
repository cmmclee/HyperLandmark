package zeusees.tracking;


public class Face {

    Face(int x1,int y1,int _width,int _height,int[] landmark,int id)
    {
        left= x1;
        top = y1;
        right = x1 + _width;
        bottom = y1 + _height;
        width = _width;
        height  = _height;
        landmarks = landmark;
        ID = id;
    }


    public int ID;

    public int left;
    public int top;
    public int right;
    public int bottom;
    public int height;
    public int width;
    public int[] landmarks;


}
