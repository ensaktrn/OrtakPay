"use client";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { useCreateGroup } from "@/hooks/useCreateGroup";
import { extractErrorMessage } from "@/lib/api";
import { zodResolver } from "@hookform/resolvers/zod";
import { useRouter } from "next/navigation";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { z } from "zod";

const newGroupSchema = z.object({
  name: z.string().min(1, "Grup adı gerekli"),
});

type NewGroupFormValues = z.infer<typeof newGroupSchema>;

export default function NewGroupPage() {
  const router = useRouter();
  const createGroup = useCreateGroup();
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<NewGroupFormValues>({ resolver: zodResolver(newGroupSchema) });

  async function onSubmit(values: NewGroupFormValues) {
    try {
      const group = await createGroup.mutateAsync({ name: values.name });
      toast.success("Grup oluşturuldu");
      router.push(`/groups/${group.id}`);
    } catch (err) {
      toast.error(extractErrorMessage(err));
    }
  }

  return (
    <div className="mx-auto flex w-full max-w-sm flex-1 flex-col justify-center px-6 py-10">
      <Card>
        <CardHeader>
          <CardTitle className="text-xl font-bold">Yeni Grup</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit(onSubmit)} className="flex flex-col gap-4">
            <div className="flex flex-col gap-1.5">
              <Label htmlFor="name">Grup adı</Label>
              <Input id="name" type="text" {...register("name")} />
              {errors.name && <p className="text-sm text-destructive">{errors.name.message}</p>}
            </div>

            <Button type="submit" loading={createGroup.isPending} className="mt-2">
              Oluştur
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
