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
    return Vector3Methods[key]
end
Vector3MT.__add=function(a,b) return Vector3.new(a.X+b.X,a.Y+b.Y,a.Z+b.Z) end
Vector3MT.__sub=function(a,b) return Vector3.new(a.X-b.X,a.Y-b.Y,a.Z-b.Z) end
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

Rect={}
function Rect.new(minX,minY,maxX,maxY)
    local min=Vector2.new(minX,minY);local max=Vector2.new(maxX,maxY)
    return {Min=min,Max=max,Width=max.X-min.X,Height=max.Y-min.Y}
end
NumberRange={}
function NumberRange.new(min,max) return {Min=min or 0,Max=max or min or 0} end

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
    rawset(self,key,value)
end
function InstanceMethods:IsA(class)
    if self.ClassName==class or class=="Instance" then return true end
    if class=="BasePart" and (self.ClassName=="Part" or self.ClassName=="MeshPart") then return true end
    return false
end
function InstanceMethods:GetChildren() return table.clone(self._children) end
function InstanceMethods:GetDescendants() local t={};descendants(self,t);return t end
function InstanceMethods:FindFirstChild(name,recursive)
    for _,c in ipairs(self._children) do if c.Name==name then return c end end
    if recursive then for _,c in ipairs(self._children) do local x=c:FindFirstChild(name,true);if x then return x end end end
end
function InstanceMethods:GetFullName()
    local t={self.Name};local p=self.Parent;while p do table.insert(t,1,p.Name);p=p.Parent end;return table.concat(t,".")
end
function InstanceMethods:SetAttribute(k,v) self._attributes[k]=v end
function InstanceMethods:GetAttribute(k) return self._attributes[k] end
function InstanceMethods:Destroy() self.Parent=nil;self._destroyed=true end
function InstanceMethods:Clone()
    local c=Instance.new(self.ClassName);c.Name=self.Name
    for k,v in pairs(self._attributes) do c._attributes[k]=v end
    for _,k in ipairs({"Position","Size","Color","Anchored"}) do c[k]=self[k] end
    for _,child in ipairs(self._children) do child:Clone().Parent=c end
    return c
end
Instance={}
function Instance.new(class)
    local x=setmetatable({_isInstance=true,ClassName=class,Name=class,_parent=nil,_children={},_attributes={},
        _childAdded=signal(),_childRemoved=signal(),_descAdded=signal(),_descRemoving=signal()},InstanceMT)
    if class=="Part" or class=="MeshPart" then x.Position=Vector3.zero;x.Size=Vector3.new(4,1,4);x.Color="#4aa3c7";x.Anchored=true end
    return x
end
workspace=Instance.new("Workspace");workspace.Name="Workspace"
game={}
local services={}
function game:GetService(name)
    if name=="Workspace" then return workspace end
    if not services[name] then
        local s={Name=name}
        if name=="RunService" then function s:IsStudio() return true end end
        services[name]=s
    end
    return services[name]
end
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
