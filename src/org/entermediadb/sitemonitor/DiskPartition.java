package org.entermediadb.sitemonitor;

public class DiskPartition
{
	private String fieldName;
	private Long fieldTotalCapacity;
	private Long fieldFreePartitionSpace;
	private Long fieldUsablePartitionSpace;

	public DiskPartition(String inName, Long inTotalCapacity, Long inFreePartitionSpace, Long inUsablePartitionSpace)
	{
		fieldName = inName;
		fieldTotalCapacity = inTotalCapacity;
		fieldFreePartitionSpace = inFreePartitionSpace;
		fieldUsablePartitionSpace = inUsablePartitionSpace;
	}

	public String getName()
	{
		return fieldName;
	}

	public void setName(String inName)
	{
		fieldName = inName;
	}

	public Long getTotalCapacity()
	{
		return fieldTotalCapacity;
	}

	public void setTotalCapacity(Long inTotalCapacity)
	{
		fieldTotalCapacity = inTotalCapacity;
	}

	public Long getFreePartitionSpace()
	{
		return fieldFreePartitionSpace;
	}

	public void setFreePartitionSpace(Long inFreePartitionSpace)
	{
		fieldFreePartitionSpace = inFreePartitionSpace;
	}

	public Long getUsablePartitionSpace()
	{
		return fieldUsablePartitionSpace;
	}

	public void setUsablePartitionSpace(Long inUsablePartitionSpace)
	{
		fieldUsablePartitionSpace = inUsablePartitionSpace;
	}

}
