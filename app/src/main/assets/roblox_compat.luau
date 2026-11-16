-- Studio Mobile preview compatibility layer; not the Roblox engine.
local function signal()
    local listeners = {}
    local obj = {}
    function obj:Connect(fn)
        assert(type(fn)=="function","Connect expects a function")
        table.insert(listeners,fn)
        local connection={Connected=true}
        function connection:Disconnect()
            if not self.Connected then return end
            self.Connected=false
            for i,v in ipairs(listeners) do if v==fn then table.remove(listeners,i);break end end
        end
        return connection
    end
    function obj:Fire(...)
        for _,fn in ipairs(table.clone(listeners)) do fn(...) end
    end
    return obj
end

local Vector3Methods={}
local Vector3MT={}
Vector3MT.__index=function(self,key)
    if key=="Magnitude" then return math.sqrt(self.X*self.X+self.Y*self.Y+self.Z*self.Z) end
    if key=="Unit" then local m=self.Magnitude;return m==0 and Vector3.zero or self/m end
    return Vector3Methods[key]
end
Vector3MT.__add=function(a,b) return Vector3.new(a.X+b.X,a.Y+b.Y,a.Z+b.Z) end
Vector3MT.__sub=function(a,b) return Vector3.new(a.X-b.X,a.Y-b.Y,a.Z-b.Z) end
Vector3MT.__unm=function(a) return Vector3.new(-a.X,-a.Y,-a.Z) end
Vector3MT.__div=function(a,b) if type(b)=="number" then return Vector3.new(a.X/b,a.Y/b,a.Z/b) end;return Vector3.new(a.X/b.X,a.Y/b.Y,a.Z/b.Z) end
Vector3MT.__mul=function(a,b) if type(a)=="number" then return Vector3.new(a*b.X,a*b.Y,a*b.Z) end return Vector3.new(a.X*b,a.Y*b,a.Z*b) end
Vector3MT.__eq=function(a,b) return a.X==b.X and a.Y==b.Y and a.Z==b.Z end
function Vector3Methods:Dot(b) return self.X*b.X+self.Y*b.Y+self.Z*b.Z end
function Vector3Methods:Cross(b) return Vector3.new(self.Y*b.Z-self.Z*b.Y,self.Z*b.X-self.X*b.Z,self.X*b.Y-self.Y*b.X) end
function Vector3Methods:Lerp(b,t) return self+(b-self)*t end
Vector3={new=function(x,y,z) return setmetatable({X=x or 0,Y=y or 0,Z=z or 0},Vector3MT) end}
Vector3.zero=Vector3.new(0,0,0);Vector3.one=Vector3.new(1,1,1)
Vector3.xAxis=Vector3.new(1,0,0);Vector3.yAxis=Vector3.new(0,1,0);Vector3.zAxis=Vector3.new(0,0,1)

-- Color3 support for common Roblox scripts.
local Color3Methods={}
local Color3MT={}
Color3MT.__index=Color3Methods
local function makeColor3(r,g,b)
    return setmetatable({R=r or 0,G=g or 0,B=b or 0},Color3MT)
end
local function clamp01(v) return math.max(0,math.min(1,v)) end
Color3={}
function Color3.new(r,g,b) return makeColor3(r,g,b) end
function Color3.fromRGB(r,g,b) return makeColor3((r or 0)/255,(g or 0)/255,(b or 0)/255) end
function Color3.fromHex(hex)
    assert(type(hex)=="string","Color3.fromHex expects a string")
    hex=string.gsub(hex,"^#","")
    assert(#hex==6 and string.match(hex,"^%x%x%x%x%x%x$"),"Color3.fromHex expects RRGGBB")
    return Color3.fromRGB(
        tonumber(string.sub(hex,1,2),16),
        tonumber(string.sub(hex,3,4),16),
        tonumber(string.sub(hex,5,6),16)
    )
end
function Color3.fromHSV(h,s,v)
    h=(h or 0)%1*6;s=s or 0;v=v or 0
    local i=math.floor(h);local f=h-i
    local p=v*(1-s);local q=v*(1-s*f);local t=v*(1-s*(1-f))
    if i==0 then return makeColor3(v,t,p)
    elseif i==1 then return makeColor3(q,v,p)
    elseif i==2 then return makeColor3(p,v,t)
    elseif i==3 then return makeColor3(p,q,v)
    elseif i==4 then return makeColor3(t,p,v)
    else return makeColor3(v,p,q) end
end
function Color3Methods:Lerp(other,alpha)
    return makeColor3(
        self.R+(other.R-self.R)*alpha,
        self.G+(other.G-self.G)*alpha,
        self.B+(other.B-self.B)*alpha
    )
end
function Color3Methods:ToHSV()
    local hi=math.max(self.R,self.G,self.B)
    local lo=math.min(self.R,self.G,self.B)
    local delta=hi-lo
    local h=0
    if delta~=0 then
        if hi==self.R then h=((self.G-self.B)/delta)%6
        elseif hi==self.G then h=(self.B-self.R)/delta+2
        else h=(self.R-self.G)/delta+4 end
        h=h/6
    end
    return h,(hi==0 and 0 or delta/hi),hi
end
function Color3Methods:ToHex()
    return string.format("%02X%02X%02X",
        math.floor(clamp01(self.R)*255+0.5),
        math.floor(clamp01(self.G)*255+0.5),
        math.floor(clamp01(self.B)*255+0.5))
end
function Color3.toHSV(value) return value:ToHSV() end
Color3MT.__add=function(a,b) return makeColor3(a.R+b.R,a.G+b.G,a.B+b.B) end
Color3MT.__sub=function(a,b) return makeColor3(a.R-b.R,a.G-b.G,a.B-b.B) end
Color3MT.__mul=function(a,b)
    if type(a)=="number" then return makeColor3(a*b.R,a*b.G,a*b.B) end
    if type(b)=="number" then return makeColor3(a.R*b,a.G*b,a.B*b) end
    return makeColor3(a.R*b.R,a.G*b.G,a.B*b.B)
end
Color3MT.__div=function(a,b)
    if type(b)=="number" then return makeColor3(a.R/b,a.G/b,a.B/b) end
    return makeColor3(a.R/b.R,a.G/b.G,a.B/b.B)
end
Color3MT.__eq=function(a,b) return a.R==b.R and a.G==b.G and a.B==b.B end
Color3MT.__tostring=function(c) return string.format("Color3(%.3f, %.3f, %.3f)",c.R,c.G,c.B) end

-- Core Roblox value types commonly used by building scripts.
local Vector2Methods={}
local Vector2MT={}
Vector2MT.__index=function(self,key)
    if key=="Magnitude" then return math.sqrt(self.X*self.X+self.Y*self.Y) end
    if key=="Unit" then local m=self.Magnitude;return m==0 and Vector2.zero or self/m end
    return Vector2Methods[key]
end
Vector2MT.__add=function(a,b) return Vector2.new(a.X+b.X,a.Y+b.Y) end
Vector2MT.__sub=function(a,b) return Vector2.new(a.X-b.X,a.Y-b.Y) end
Vector2MT.__unm=function(a) return Vector2.new(-a.X,-a.Y) end
Vector2MT.__mul=function(a,b) if type(a)=="number" then return Vector2.new(a*b.X,a*b.Y) end;if type(b)=="number" then return Vector2.new(a.X*b,a.Y*b) end;return Vector2.new(a.X*b.X,a.Y*b.Y) end
Vector2MT.__div=function(a,b) if type(b)=="number" then return Vector2.new(a.X/b,a.Y/b) end;return Vector2.new(a.X/b.X,a.Y/b.Y) end
Vector2MT.__eq=function(a,b) return a.X==b.X and a.Y==b.Y end
function Vector2Methods:Dot(b) return self.X*b.X+self.Y*b.Y end
function Vector2Methods:Lerp(b,t) return self+(b-self)*t end
Vector2={new=function(x,y) return setmetatable({X=x or 0,Y=y or 0},Vector2MT) end}
Vector2.zero=Vector2.new(0,0);Vector2.one=Vector2.new(1,1)
Vector2.xAxis=Vector2.new(1,0);Vector2.yAxis=Vector2.new(0,1)

local CFrameMT
local function makeCFrame(position,r00,r01,r02,r10,r11,r12,r20,r21,r22)
    return setmetatable({_position=position or Vector3.zero,
        R00=r00 or 1,R01=r01 or 0,R02=r02 or 0,
        R10=r10 or 0,R11=r11 or 1,R12=r12 or 0,
        R20=r20 or 0,R21=r21 or 0,R22=r22 or 1},CFrameMT)
end
local CFrameMethods={}
CFrameMT={}
CFrameMT.__index=function(self,key)
    if key=="Position" then return self._position end
    if key=="X" then return self._position.X end
    if key=="Y" then return self._position.Y end
    if key=="Z" then return self._position.Z end
    if key=="RightVector" then return Vector3.new(self.R00,self.R10,self.R20) end
    if key=="UpVector" then return Vector3.new(self.R01,self.R11,self.R21) end
    if key=="LookVector" then return Vector3.new(-self.R02,-self.R12,-self.R22) end
    if key=="Rotation" then return makeCFrame(Vector3.zero,self.R00,self.R01,self.R02,self.R10,self.R11,self.R12,self.R20,self.R21,self.R22) end
    return CFrameMethods[key]
end
local function multiplyCFrame(a,b)
    local p=a._position+a:VectorToWorldSpace(b._position)
    return makeCFrame(p,
        a.R00*b.R00+a.R01*b.R10+a.R02*b.R20,a.R00*b.R01+a.R01*b.R11+a.R02*b.R21,a.R00*b.R02+a.R01*b.R12+a.R02*b.R22,
        a.R10*b.R00+a.R11*b.R10+a.R12*b.R20,a.R10*b.R01+a.R11*b.R11+a.R12*b.R21,a.R10*b.R02+a.R11*b.R12+a.R12*b.R22,
        a.R20*b.R00+a.R21*b.R10+a.R22*b.R20,a.R20*b.R01+a.R21*b.R11+a.R22*b.R21,a.R20*b.R02+a.R21*b.R12+a.R22*b.R22)
end
CFrameMT.__mul=function(a,b)
    if getmetatable(b)==CFrameMT then return multiplyCFrame(a,b) end
    if getmetatable(b)==Vector3MT then return a:PointToWorldSpace(b) end
    error("CFrame can only multiply another CFrame or Vector3")
end
CFrameMT.__add=function(a,b) return makeCFrame(a._position+b,a.R00,a.R01,a.R02,a.R10,a.R11,a.R12,a.R20,a.R21,a.R22) end
CFrameMT.__sub=function(a,b) return makeCFrame(a._position-b,a.R00,a.R01,a.R02,a.R10,a.R11,a.R12,a.R20,a.R21,a.R22) end
CFrameMT.__tostring=function(a) return "CFrame("..tostring(a.X)..", "..tostring(a.Y)..", "..tostring(a.Z)..")" end
CFrame={}
function CFrame.new(...)
    local args={...}
    if #args==0 then return makeCFrame(Vector3.zero) end
    if getmetatable(args[1])==Vector3MT then
        if getmetatable(args[2])==Vector3MT then return CFrame.lookAt(args[1],args[2]) end
        return makeCFrame(args[1])
    end
    if #args>=12 then return makeCFrame(Vector3.new(args[1],args[2],args[3]),table.unpack(args,4,12)) end
    return makeCFrame(Vector3.new(args[1] or 0,args[2] or 0,args[3] or 0))
end
function CFrame.Angles(rx,ry,rz)
    local sx,cx=math.sin(rx or 0),math.cos(rx or 0)
    local sy,cy=math.sin(ry or 0),math.cos(ry or 0)
    local sz,cz=math.sin(rz or 0),math.cos(rz or 0)
    return makeCFrame(Vector3.zero,
        cz*cy,cz*sy*sx-sz*cx,cz*sy*cx+sz*sx,
        sz*cy,sz*sy*sx+cz*cx,sz*sy*cx-cz*sx,
        -sy,cy*sx,cy*cx)
end
CFrame.fromEulerAnglesXYZ=CFrame.Angles
CFrame.fromOrientation=CFrame.Angles
function CFrame.lookAt(eye,target,up)
    up=up or Vector3.yAxis
    local back=(eye-target).Unit
    if back.Magnitude==0 then back=Vector3.zAxis end
    local right=up:Cross(back).Unit
    if right.Magnitude==0 then right=Vector3.xAxis end
    local realUp=back:Cross(right).Unit
    return makeCFrame(eye,right.X,realUp.X,back.X,right.Y,realUp.Y,back.Y,right.Z,realUp.Z,back.Z)
end
CFrame.identity=CFrame.new()
function CFrameMethods:GetComponents()
    return self.X,self.Y,self.Z,self.R00,self.R01,self.R02,self.R10,self.R11,self.R12,self.R20,self.R21,self.R22
end
function CFrameMethods:VectorToWorldSpace(v)
    return Vector3.new(self.R00*v.X+self.R01*v.Y+self.R02*v.Z,self.R10*v.X+self.R11*v.Y+self.R12*v.Z,self.R20*v.X+self.R21*v.Y+self.R22*v.Z)
end
function CFrameMethods:VectorToObjectSpace(v)
    return Vector3.new(self.R00*v.X+self.R10*v.Y+self.R20*v.Z,self.R01*v.X+self.R11*v.Y+self.R21*v.Z,self.R02*v.X+self.R12*v.Y+self.R22*v.Z)
end
function CFrameMethods:PointToWorldSpace(v) return self._position+self:VectorToWorldSpace(v) end
function CFrameMethods:PointToObjectSpace(v) return self:VectorToObjectSpace(v-self._position) end
function CFrameMethods:ToWorldSpace(other) return self*other end
function CFrameMethods:ToObjectSpace(other) return self:Inverse()*other end
function CFrameMethods:Inverse()
    local p=Vector3.new(
        -(self.R00*self.X+self.R10*self.Y+self.R20*self.Z),
        -(self.R01*self.X+self.R11*self.Y+self.R21*self.Z),
        -(self.R02*self.X+self.R12*self.Y+self.R22*self.Z))
    return makeCFrame(p,self.R00,self.R10,self.R20,self.R01,self.R11,self.R21,self.R02,self.R12,self.R22)
end
function CFrameMethods:Lerp(other,t)
    local function mix(a,b) return a+(b-a)*t end
    return makeCFrame(self._position:Lerp(other._position,t),
        mix(self.R00,other.R00),mix(self.R01,other.R01),mix(self.R02,other.R02),
        mix(self.R10,other.R10),mix(self.R11,other.R11),mix(self.R12,other.R12),
        mix(self.R20,other.R20),mix(self.R21,other.R21),mix(self.R22,other.R22))
end
function CFrameMethods:ToOrientation()
    local y=math.asin(math.max(-1,math.min(1,-self.R20)))
    local x,z
    if math.abs(self.R20)<0.999999 then x=math.atan2(self.R21,self.R22);z=math.atan2(self.R10,self.R00)
    else x=math.atan2(-self.R12,self.R11);z=0 end
    return x,y,z
end

local UDimMT={}
UDimMT.__index=UDimMT
UDimMT.__add=function(a,b) return UDim.new(a.Scale+b.Scale,a.Offset+b.Offset) end
UDimMT.__sub=function(a,b) return UDim.new(a.Scale-b.Scale,a.Offset-b.Offset) end
UDim={}
function UDim.new(scale,offset) return setmetatable({Scale=scale or 0,Offset=offset or 0},UDimMT) end
local UDim2MT={}
UDim2MT.__index=UDim2MT
UDim2MT.__add=function(a,b) return UDim2.new(a.X.Scale+b.X.Scale,a.X.Offset+b.X.Offset,a.Y.Scale+b.Y.Scale,a.Y.Offset+b.Y.Offset) end
UDim2MT.__sub=function(a,b) return UDim2.new(a.X.Scale-b.X.Scale,a.X.Offset-b.X.Offset,a.Y.Scale-b.Y.Scale,a.Y.Offset-b.Y.Offset) end
UDim2={}
function UDim2.new(xs,xo,ys,yo) return setmetatable({X=UDim.new(xs,xo),Y=UDim.new(ys,yo)},UDim2MT) end
function UDim2.fromScale(x,y) return UDim2.new(x,0,y,0) end
function UDim2.fromOffset(x,y) return UDim2.new(0,x,0,y) end
UDim2.zero=UDim2.new(0,0,0,0)

TweenInfo={}
function TweenInfo.new(duration,style,direction,repeatCount,reverses,delayTime)
    return {Time=duration or 1,EasingStyle=style or Enum.EasingStyle.Quad,
        EasingDirection=direction or Enum.EasingDirection.Out,RepeatCount=repeatCount or 0,
        Reverses=reverses or false,DelayTime=delayTime or 0}
end
local NumberSequenceKeypoint={}
function NumberSequenceKeypoint.new(time,value,envelope) return {Time=time,Value=value,Envelope=envelope or 0} end
NumberSequence={}
function NumberSequence.new(value)
    if type(value)=="number" then return {Keypoints={NumberSequenceKeypoint.new(0,value),NumberSequenceKeypoint.new(1,value)}} end
    return {Keypoints=value}
end
local ColorSequenceKeypoint={}
function ColorSequenceKeypoint.new(time,value) return {Time=time,Value=value} end
ColorSequence={}
function ColorSequence.new(a,b)
    if b then return {Keypoints={ColorSequenceKeypoint.new(0,a),ColorSequenceKeypoint.new(1,b)}} end
    if getmetatable(a)==Color3MT then return {Keypoints={ColorSequenceKeypoint.new(0,a),ColorSequenceKeypoint.new(1,a)}} end
    return {Keypoints=a}
end

Rect={}
function Rect.new(minX,minY,maxX,maxY)
    local min=Vector2.new(minX,minY);local max=Vector2.new(maxX,maxY)
    return {Min=min,Max=max,Width=max.X-min.X,Height=max.Y-min.Y}
end
NumberRange={}
function NumberRange.new(min,max) return {Min=min or 0,Max=max or min or 0} end

RaycastParams={}
function RaycastParams.new()
    return {FilterType=Enum.RaycastFilterType.Exclude,FilterDescendantsInstances={},
        IgnoreWater=false,CollisionGroup="Default",RespectCanCollide=false, BruteForceAllSlow=false}
end
OverlapParams={}
function OverlapParams.new()
    return {FilterType=Enum.RaycastFilterType.Exclude,FilterDescendantsInstances={},
        CollisionGroup="Default",RespectCanCollide=false,MaxParts=0}
end
local function isFiltered(part,params)
    if not params then return false end
    local listed=false
    for _,root in ipairs(params.FilterDescendantsInstances or {}) do
        if part==root or part:IsDescendantOf(root) then listed=true;break end
    end
    local filter=params.FilterType
    local include=(filter==Enum.RaycastFilterType.Include or filter==Enum.RaycastFilterType.Whitelist)
    return include and not listed or not include and listed
end
local function rayBox(origin,direction,center,size)
    local min={center.X-size.X/2,center.Y-size.Y/2,center.Z-size.Z/2}
    local max={center.X+size.X/2,center.Y+size.Y/2,center.Z+size.Z/2}
    local o={origin.X,origin.Y,origin.Z};local d={direction.X,direction.Y,direction.Z}
    local tmin,tmax=-math.huge,math.huge;local normal=Vector3.zero
    for axis=1,3 do
        if math.abs(d[axis])<1e-9 then
            if o[axis]<min[axis] or o[axis]>max[axis] then return nil end
        else
            local a=(min[axis]-o[axis])/d[axis]
            local b=(max[axis]-o[axis])/d[axis]
            local sign=-1
            if a>b then a,b=b,a;sign=1 end
            if a>tmin then
                tmin=a
                normal=axis==1 and Vector3.new(sign,0,0) or axis==2 and Vector3.new(0,sign,0) or Vector3.new(0,0,sign)
            end
            tmax=math.min(tmax,b)
            if tmin>tmax then return nil end
        end
    end
    local t=tmin>=0 and tmin or tmax
    if t<0 or t>1 then return nil end
    return t,normal
end
local function allParts(root)
    local result={}
    for _,item in ipairs(root:GetDescendants()) do if item:IsA("BasePart") then table.insert(result,item) end end
    return result
end
function InstanceMethods:Raycast(origin,direction,params)
    assert(self.ClassName=="Workspace","Raycast is only available on Workspace")
    local length=direction.Magnitude
    if length==0 then return nil end
    local best,bestNormal,bestT=nil,nil,math.huge
    for _,part in ipairs(allParts(self)) do
        if part.CanQuery~=false and not isFiltered(part,params) then
            local t,normal=rayBox(origin,direction,part.Position,part.Size)
            if t and t<bestT then best,bestNormal,bestT=part,normal,t end
        end
    end
    if not best then return nil end
    return {Instance=best,Position=origin+direction*bestT,Normal=bestNormal,
        Distance=length*bestT,Material=best.Material or Enum.Material.Plastic}
end
function InstanceMethods:GetPartBoundsInRadius(position,radius,params)
    local result={}
    for _,part in ipairs(allParts(self)) do
        if not isFiltered(part,params) then
            local closest=Vector3.new(
                math.max(part.Position.X-part.Size.X/2,math.min(position.X,part.Position.X+part.Size.X/2)),
                math.max(part.Position.Y-part.Size.Y/2,math.min(position.Y,part.Position.Y+part.Size.Y/2)),
                math.max(part.Position.Z-part.Size.Z/2,math.min(position.Z,part.Position.Z+part.Size.Z/2)))
            if (closest-position).Magnitude<=radius then table.insert(result,part) end
        end
    end
    return result
end
function InstanceMethods:GetPartBoundsInBox(cf,size,params)
    local result={};local center=cf.Position
    for _,part in ipairs(allParts(self)) do
        if not isFiltered(part,params) and
            math.abs(part.Position.X-center.X)<=(part.Size.X+size.X)/2 and
            math.abs(part.Position.Y-center.Y)<=(part.Size.Y+size.Y)/2 and
            math.abs(part.Position.Z-center.Z)<=(part.Size.Z+size.Z)/2 then table.insert(result,part) end
    end
    return result
end

-- Enum members are lightweight typed values; unsupported engine behavior is not simulated.
local EnumTypes={}
local EnumTypeMT={}
EnumTypeMT.__index=function(self,key)
    if key=="GetEnumItems" then return function()
        local result={};for _,item in pairs(self._items) do table.insert(result,item) end
        table.sort(result,function(a,b) return a.Value<b.Value end);return result
    end end
    local item=self._items[key]
    if not item then item={Name=key,Value=#self._items,EnumType=self};setmetatable(item,{__tostring=function(v) return "Enum."..self.Name.."."..v.Name end});self._items[key]=item end
    return item
end
EnumTypeMT.__tostring=function(self) return "Enum."..self.Name end
Enum=setmetatable({}, {__index=function(_,name)
    if not EnumTypes[name] then EnumTypes[name]=setmetatable({Name=name,_items={}},EnumTypeMT) end
    return EnumTypes[name]
end})

local brickNames={White={255,255,255},Black={27,42,53},Really_red={255,0,0},Bright_red={196,40,28},Bright_blue={13,105,172},Bright_green={75,151,75},New_Yeller={255,255,0},Bright_yellow={245,205,48},Institutional_white={248,248,248},Medium_stone_grey={163,162,165},Dark_stone_grey={99,95,98},Really_black={17,17,17}}
BrickColor={}
local function makeBrickColor(name,r,g,b)
    local color=Color3.fromRGB(r,g,b)
    return {Name=name,Number=0,Color=color, __type="BrickColor"}
end
function BrickColor.new(value)
    if type(value)=="string" then local rgb=brickNames[string.gsub(value," ","_")];if rgb then return makeBrickColor(value,rgb[1],rgb[2],rgb[3]) end end
    if type(value)=="number" then return makeBrickColor("BrickColor",value%256,(value*3)%256,(value*7)%256) end
    if getmetatable(value)==Color3MT then return BrickColor.fromColor3(value) end
    return makeBrickColor("Medium stone grey",163,162,165)
end
function BrickColor.fromRGB(r,g,b) return BrickColor.fromColor3(Color3.fromRGB(r,g,b)) end
function BrickColor.fromColor3(color) return makeBrickColor(color:ToHex(),color.R*255,color.G*255,color.B*255) end
BrickColor.White=BrickColor.new("White");BrickColor.Black=BrickColor.new("Black")
BrickColor.Gray=BrickColor.new("Medium stone grey");BrickColor.Red=BrickColor.new("Bright red")

local InstanceMethods={}
local InstanceMT={}
local function descendants(root,out) for _,c in ipairs(root._children) do table.insert(out,c);descendants(c,out) end end
InstanceMT.__index=function(self,key)
    if key=="Parent" then return rawget(self,"_parent") end
    if key=="ChildAdded" then return rawget(self,"_childAdded") end
    if key=="ChildRemoved" then return rawget(self,"_childRemoved") end
    if key=="DescendantAdded" then return rawget(self,"_descAdded") end
    if key=="DescendantRemoving" then return rawget(self,"_descRemoving") end
    if key=="Changed" then return rawget(self,"_changed") end
    if key=="Touched" then return rawget(self,"_touched") end
    if key=="TouchEnded" then return rawget(self,"_touchEnded") end
    return InstanceMethods[key] or rawget(self,key)
end
InstanceMT.__newindex=function(self,key,value)
    if key=="Parent" then
        local old=rawget(self,"_parent");if old==value then return end
        local p=value;while p do assert(p~=self,"Parent cycle");p=p._parent end
        if old then
            old._childRemoved:Fire(self)
            local removed={};descendants(self,removed);table.insert(removed,1,self)
            local root=old;while root._parent do root=root._parent end
            for _,d in ipairs(removed) do root._descRemoving:Fire(d) end
            for i,c in ipairs(old._children) do if c==self then table.remove(old._children,i);break end end
        end
        rawset(self,"_parent",value)
        if value then
            table.insert(value._children,self);value._childAdded:Fire(self)
            local added={};descendants(self,added);table.insert(added,1,self)
            local root=value;while root._parent do root=root._parent end
            for _,d in ipairs(added) do root._descAdded:Fire(d) end
        end
        return
    end
    local previous=rawget(self,key)
    rawset(self,key,value)
    if key=="CFrame" and getmetatable(value)==CFrameMT then
        rawset(self,"Position",value.Position)
    elseif key=="Position" and getmetatable(value)==Vector3MT then
        local cf=rawget(self,"CFrame")
        if cf then rawset(self,"CFrame",makeCFrame(value,cf.R00,cf.R01,cf.R02,cf.R10,cf.R11,cf.R12,cf.R20,cf.R21,cf.R22)) end
    end
    if previous~=value then
        if self._changed then self._changed:Fire(key) end
        local changed=self._propertySignals and self._propertySignals[key]
        if changed then changed:Fire() end
    end
end
function InstanceMethods:IsA(class)
    if self.ClassName==class or class=="Instance" then return true end
    local inheritance={
        Part={"BasePart","PVInstance"},MeshPart={"BasePart","PVInstance"},UnionOperation={"BasePart","PVInstance"},
        Model={"PVInstance"},Tool={"BackpackItem"},HopperBin={"BackpackItem"},
        Frame={"GuiObject","GuiBase2d"},TextLabel={"GuiObject","GuiBase2d"},TextButton={"GuiButton","GuiObject","GuiBase2d"},
        ImageLabel={"GuiObject","GuiBase2d"},ImageButton={"GuiButton","GuiObject","GuiBase2d"},
        ScreenGui={"LayerCollector","GuiBase2d"},BillboardGui={"LayerCollector","GuiBase3d"}
    }
    for _,base in ipairs(inheritance[self.ClassName] or {}) do if base==class then return true end end
    return false
end
function InstanceMethods:GetChildren() return table.clone(self._children) end
function InstanceMethods:GetDescendants() local t={};descendants(self,t);return t end
function InstanceMethods:FindFirstChildOfClass(class)
    for _,child in ipairs(self._children) do if child:IsA(class) then return child end end
end
function InstanceMethods:FindFirstChildWhichIsA(class,recursive)
    for _,child in ipairs(self._children) do if child:IsA(class) then return child end end
    if recursive then for _,child in ipairs(self._children) do local found=child:FindFirstChildWhichIsA(class,true);if found then return found end end end
end
function InstanceMethods:WaitForChild(name,_timeout) return self:FindFirstChild(name) end
function InstanceMethods:IsDescendantOf(ancestor)
    local parent=self.Parent
    while parent do if parent==ancestor then return true end;parent=parent.Parent end
    return false
end
function InstanceMethods:GetPropertyChangedSignal(name)
    self._propertySignals[name]=self._propertySignals[name] or signal()
    return self._propertySignals[name]
end
function InstanceMethods:GetAttributeChangedSignal(name)
    self._attributeSignals[name]=self._attributeSignals[name] or signal()
    return self._attributeSignals[name]
end
function InstanceMethods:GetAttributes()
    local result={};for key,value in pairs(self._attributes) do result[key]=value end;return result
end
function InstanceMethods:ClearAllChildren()
    for _,child in ipairs(table.clone(self._children)) do child:Destroy() end
end
function InstanceMethods:FindFirstChild(name,recursive)
    for _,c in ipairs(self._children) do if c.Name==name then return c end end
    if recursive then for _,c in ipairs(self._children) do local x=c:FindFirstChild(name,true);if x then return x end end end
end
function InstanceMethods:GetFullName()
    local t={self.Name};local p=self.Parent;while p do table.insert(t,1,p.Name);p=p.Parent end;return table.concat(t,".")
end
function InstanceMethods:SetAttribute(k,v)
    local old=self._attributes[k];self._attributes[k]=v
    if old~=v and self._attributeSignals[k] then self._attributeSignals[k]:Fire() end
end
function InstanceMethods:GetAttribute(k) return self._attributes[k] end
function InstanceMethods:RemoveAttribute(k) self:SetAttribute(k,nil) end
function InstanceMethods:Destroy() self:ClearAllChildren();self.Parent=nil;self._destroyed=true end
function InstanceMethods:GetPivot()
    if self:IsA("BasePart") then return self.CFrame end
    return self._pivot or CFrame.new()
end
function InstanceMethods:PivotTo(target)
    assert(getmetatable(target)==CFrameMT,"PivotTo expects a CFrame")
    self._pivot=target
    if self:IsA("BasePart") then self.CFrame=target;return end
    local delta=target*self:GetPivot():Inverse()
    for _,child in ipairs(self:GetDescendants()) do
        if child:IsA("BasePart") then child.CFrame=delta*child.CFrame end
    end
end
function InstanceMethods:MoveTo(position)
    if self:IsA("BasePart") then self.Position=position;return end
    local pivot=self:GetPivot();self:PivotTo(CFrame.new(position)*pivot.Rotation)
end
function InstanceMethods:GetTouchingParts() return {} end
function InstanceMethods:BreakJoints() end

function InstanceMethods:Fire(...)
    assert(self.ClassName=="BindableEvent","Fire is only valid on BindableEvent")
    self.Event:Fire(...)
end
function InstanceMethods:FireServer(...)
    assert(self.ClassName=="RemoteEvent","FireServer is only valid on RemoteEvent")
    self.OnServerEvent:Fire(game:GetService("Players").LocalPlayer,...)
end
function InstanceMethods:FireClient(player,...)
    assert(self.ClassName=="RemoteEvent","FireClient is only valid on RemoteEvent")
    if player==game:GetService("Players").LocalPlayer then self.OnClientEvent:Fire(...) end
end
function InstanceMethods:FireAllClients(...)
    assert(self.ClassName=="RemoteEvent","FireAllClients is only valid on RemoteEvent")
    self.OnClientEvent:Fire(...)
end
function InstanceMethods:InvokeServer(...)
    assert(self.ClassName=="RemoteFunction","InvokeServer is only valid on RemoteFunction")
    assert(type(self.OnServerInvoke)=="function","OnServerInvoke is not set")
    return self.OnServerInvoke(game:GetService("Players").LocalPlayer,...)
end
function InstanceMethods:InvokeClient(player,...)
    assert(self.ClassName=="RemoteFunction","InvokeClient is only valid on RemoteFunction")
    assert(type(self.OnClientInvoke)=="function","OnClientInvoke is not set")
    if player==game:GetService("Players").LocalPlayer then return self.OnClientInvoke(...) end
end
function InstanceMethods:Invoke(...)
    assert(self.ClassName=="BindableFunction","Invoke is only valid on BindableFunction")
    assert(type(self.OnInvoke)=="function","OnInvoke is not set")
    return self.OnInvoke(...)
end
function InstanceMethods:Clone()
    local c=Instance.new(self.ClassName);c.Name=self.Name
    for k,v in pairs(self._attributes) do c._attributes[k]=v end
    for k,v in pairs(self) do
        if string.sub(k,1,1)~="_" and k~="ClassName" and k~="Name" and k~="Parent" then c[k]=v end
    end
    for _,child in ipairs(self._children) do child:Clone().Parent=c end
    return c
end
Instance={}
function Instance.new(class)
    local x=setmetatable({_isInstance=true,ClassName=class,Name=class,_parent=nil,_children={},_attributes={},
        _propertySignals={},_attributeSignals={},_childAdded=signal(),_childRemoved=signal(),_descAdded=signal(),
        _descRemoving=signal(),_changed=signal(),_touched=signal(),_touchEnded=signal()},InstanceMT)
    if class=="Part" or class=="MeshPart" or class=="UnionOperation" then
        x.Position=Vector3.zero;x.CFrame=CFrame.new();x.Size=Vector3.new(4,1,4)
        x.Color=Color3.fromRGB(74,163,199);x.Anchored=true;x.CanCollide=true;x.CanTouch=true
        x.CanQuery=true;x.Transparency=0;x.Reflectance=0;x.Material=Enum.Material.Plastic
    elseif class=="Model" then x._pivot=CFrame.new()
    elseif class=="Humanoid" then x.Health=100;x.MaxHealth=100;x.WalkSpeed=16;x.JumpPower=50;x.HipHeight=2
    elseif class=="Frame" or class=="TextLabel" or class=="TextButton" or class=="ImageLabel" or class=="ImageButton" then
        x.Size=UDim2.new(0,100,0,50);x.Position=UDim2.zero;x.Visible=true;x.BackgroundTransparency=0
    elseif class=="ScreenGui" then x.Enabled=true;x.ResetOnSpawn=true
    elseif class=="RemoteEvent" then x.OnServerEvent=signal();x.OnClientEvent=signal()
    elseif class=="BindableEvent" then x.Event=signal()
    elseif class=="RemoteFunction" then x.OnServerInvoke=nil;x.OnClientInvoke=nil
    elseif class=="BindableFunction" then x.OnInvoke=nil
    end
    return x
end
local function serviceInstance(name)
    local service=Instance.new("Folder")
    rawset(service,"ClassName",name)
    service.Name=name
    return service
end
workspace=Instance.new("Workspace");workspace.Name="Workspace";workspace.Gravity=196.2
workspace.CurrentCamera=Instance.new("Camera")
workspace.CurrentCamera.Name="Camera"
workspace.CurrentCamera.CFrame=CFrame.new(0,5,16)
workspace.CurrentCamera.FieldOfView=70
workspace.CurrentCamera.ViewportSize=Vector2.new(360,720)
game=Instance.new("DataModel")
game.Name="game";game.PlaceId=0;game.JobId="studio-mobile-preview";game.GameId=0
local services={}
local playersService=serviceInstance("Players")
playersService.PlayerAdded=signal();playersService.PlayerRemoving=signal();playersService.CharacterAutoLoads=true
local localPlayer=Instance.new("Player")
localPlayer.Name="LocalPlayer";localPlayer.DisplayName="Local Player";localPlayer.UserId=1
localPlayer.AccountAge=0;localPlayer.MembershipType=Enum.MembershipType.None
localPlayer.PlayerGui=Instance.new("PlayerGui")
localPlayer.PlayerGui.Name="PlayerGui";localPlayer.PlayerGui.Parent=localPlayer
localPlayer.Character=Instance.new("Model");localPlayer.Character.Name="Character"
local rootPart=Instance.new("Part");rootPart.Name="HumanoidRootPart";rootPart.Parent=localPlayer.Character
local humanoid=Instance.new("Humanoid");humanoid.Parent=localPlayer.Character
localPlayer.Parent=playersService
function localPlayer:FindFirstChild(name,recursive)
    local direct=InstanceMethods.FindFirstChild(self,name,recursive)
    if direct then return direct end
    return self.Character and self.Character:FindFirstChild(name,recursive)
end
function playersService:GetPlayers() return self:GetChildren() end
function playersService:GetPlayerFromCharacter(character)
    for _,player in ipairs(self:GetPlayers()) do if player.Character==character then return player end end
end
playersService.LocalPlayer=localPlayer
local runService=serviceInstance("RunService")
runService.Heartbeat=signal();runService.RenderStepped=signal();runService.Stepped=signal()
function runService:IsStudio() return true end
function runService:IsClient() return true end
function runService:IsServer() return false end
function runService:IsRunning() return true end
local tweenService=serviceInstance("TweenService")
function tweenService:Create(object,info,goals)
    local tween={Instance=object,Info=info,PlaybackState=Enum.PlaybackState.Begin,Completed=signal()}
    function tween:Play()
        for key,value in pairs(goals or {}) do object[key]=value end
        self.PlaybackState=Enum.PlaybackState.Completed
        self.Completed:Fire(Enum.PlaybackState.Completed)
    end
    function tween:Cancel() self.PlaybackState=Enum.PlaybackState.Cancelled end
    function tween:Pause() self.PlaybackState=Enum.PlaybackState.Paused end
    function tween:Destroy() end
    return tween
end
local debris=serviceInstance("Debris")
function debris:AddItem(object,lifetime) if lifetime and lifetime<=0 and object and object.Destroy then object:Destroy() end end
local collectionService=serviceInstance("CollectionService")
collectionService._tags={}
function collectionService:AddTag(object,tag)
    self._tags[tag]=self._tags[tag] or {};self._tags[tag][object]=true
end
function collectionService:RemoveTag(object,tag) if self._tags[tag] then self._tags[tag][object]=nil end end
function collectionService:HasTag(object,tag) return self._tags[tag] and self._tags[tag][object]==true or false end
function collectionService:GetTags(object)
    local result={};for tag,objects in pairs(self._tags) do if objects[object] then table.insert(result,tag) end end;return result
end
function collectionService:GetTagged(tag)
    local result={};for object in pairs(self._tags[tag] or {}) do table.insert(result,object) end;return result
end
local function makeFolderService(name)
    return serviceInstance(name)
end
local lighting=makeFolderService("Lighting");lighting.Brightness=2;lighting.ClockTime=14;lighting.FogEnd=100000
local replicatedStorage=makeFolderService("ReplicatedStorage")
local serverStorage=makeFolderService("ServerStorage")
local serverScriptService=makeFolderService("ServerScriptService")
local starterGui=makeFolderService("StarterGui")
local starterPack=makeFolderService("StarterPack")
local soundService=makeFolderService("SoundService")
local teams=makeFolderService("Teams")
local dataStores={}
local function getDataStore(name)
    if dataStores[name] then return dataStores[name] end
    local values={}
    local store=serviceInstance("DataStore")
    store.Name=name
    function store:GetAsync(key) return values[tostring(key)] end
    function store:SetAsync(key,value) values[tostring(key)]=value end
    function store:RemoveAsync(key) local old=values[tostring(key)];values[tostring(key)]=nil;return old end
    function store:UpdateAsync(key,transform) local k=tostring(key);local nextValue=transform(values[k]);values[k]=nextValue;return nextValue end
    dataStores[name]=store
    return store
end
local dataStoreService=serviceInstance("DataStoreService")
function dataStoreService:GetDataStore(name)
    local key=tostring(name)
    if not dataStores[key] then dataStores[key]=getDataStore(key) end
    return dataStores[key]
end
function dataStoreService:GetGlobalDataStore() return self:GetDataStore("Global") end
local known={
    Workspace=workspace,Players=playersService,RunService=runService,TweenService=tweenService,Debris=debris,
    CollectionService=collectionService,Lighting=lighting,ReplicatedStorage=replicatedStorage,
    ServerStorage=serverStorage,ServerScriptService=serverScriptService,StarterGui=starterGui,
    StarterPack=starterPack,SoundService=soundService,Teams=teams,DataStoreService=dataStoreService
}
for name,service in pairs(known) do service.Parent=game end
function game:GetService(name)
    if known[name] then return known[name] end
    if not services[name] then
        services[name]=serviceInstance(name)
        services[name].Parent=game
    end
    return services[name]
end
function game:FindService(name) return self:GetService(name) end
function game:IsLoaded() return true end

function typeof(v)
    if type(v)=="table" then if v._isInstance then return "Instance" end;if getmetatable(v)==Vector3MT then return "Vector3" end;if getmetatable(v)==Vector2MT then return "Vector2" end;if getmetatable(v)==Color3MT then return "Color3" end;if getmetatable(v)==CFrameMT then return "CFrame" end end
    return type(v)
end
script=Instance.new("Script");script.Name="PreviewScript"
task={}
function task.wait(_) return 0 end
function task.spawn(fn,...) return fn(...) end
function task.defer(fn,...) return fn(...) end
function task.delay(_,fn,...) return fn(...) end
function task.cancel(_) end
wait=task.wait
spawn=task.spawn
delay=task.delay
function tick() return 0 end
function time() return 0 end
function warn(...) print("[Warn]",...) end
