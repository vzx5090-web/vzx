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
    if type(v)=="table" then if v._isInstance then return "Instance" end;if getmetatable(v)==Vector3MT then return "Vector3" end;if getmetatable(v)==Color3MT then return "Color3" end end
    return type(v)
end
script=Instance.new("Script");script.Name="PreviewScript"
task={}
function task.wait(_) return 0 end
function task.spawn(fn,...) return fn(...) end
function task.defer(fn,...) return fn(...) end
function task.delay(_,fn,...) return fn(...) end
function task.cancel(_) end
