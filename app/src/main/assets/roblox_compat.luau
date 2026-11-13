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
    if type(v)=="table" then if v._isInstance then return "Instance" end;if getmetatable(v)==Vector3MT then return "Vector3" end end
    return type(v)
end
script=Instance.new("Script");script.Name="PreviewScript"
task={}
function task.wait(_) return 0 end
function task.spawn(fn,...) return fn(...) end
function task.defer(fn,...) return fn(...) end
function task.delay(_,fn,...) return fn(...) end
function task.cancel(_) end
